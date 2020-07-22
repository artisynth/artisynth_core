/**
 * Copyright (c) 2017 by the Authors: John E Lloyd (UBC), Antonio Sanchez (UBC),
 * and ArtiSynth Team Members. Elliptic selection added by Doga Tekin (ETH).
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render.GL;

import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.KeyListener;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLEventListener;
import javax.swing.event.MouseInputListener;

import maspack.matrix.AffineTransform2d;
import maspack.matrix.AffineTransform2dBase;
import maspack.matrix.AffineTransform3d;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.AxisAlignedRotation;
import maspack.matrix.AxisAngle;
import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix3dBase;
import maspack.matrix.Matrix4d;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform2d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.SVDecomposition3d;
import maspack.matrix.Vector2d;
import maspack.matrix.Vector3d;
import maspack.properties.HasProperties;
import maspack.properties.Property;
import maspack.properties.PropertyList;
import maspack.render.BumpMapProps;
import maspack.render.ColorMapProps;
import maspack.render.Dragger3d;
import maspack.render.Dragger3d.DraggerType;
import maspack.render.Dragger3dBase;
import maspack.render.DrawToolBase;
import maspack.render.IsRenderable;
import maspack.render.IsSelectable;
import maspack.render.Light;
import maspack.render.Material;
import maspack.render.MouseRayEvent;
import maspack.render.NormalMapProps;
import maspack.render.RenderList;
import maspack.render.RenderListener;
import maspack.render.RenderObject;
import maspack.render.RenderProps;
import maspack.render.RendererEvent;
import maspack.render.SortedRenderableList;
import maspack.render.VertexIndexArray;
import maspack.render.Viewer;
import maspack.render.ViewerSelectionEvent;
import maspack.render.ViewerSelectionFilter;
import maspack.render.ViewerSelectionListener;
import maspack.render.GL.GLProgramInfo.RenderingMode;
import maspack.render.GL.GLGridPlane.AxisLabeling;
import maspack.util.FunctionTimer;
import maspack.util.InternalErrorException;
import maspack.util.Logger;

/**
 * @author John E Lloyd and ArtiSynth team members
 */
public abstract class GLViewer implements GLEventListener, GLRenderer, 
   HasProperties, Viewer {

   public enum GLVersion {
      GL2(2, 1), 
      GL3(3, 3);
      
      int major;
      int minor;
      
      GLVersion(int major, int minor) {
         this.major = major;
         this.minor = minor;
      }
      
      public int getMajorVersion() {
         return major;
      }
      
      public int getMinorVersion() {
         return minor;
      }
   }
   
   /**
    * Whether to use a GLJPanel or GLCanvas
    */
   public static boolean useGLJPanel = false;
   
   // Disposal
   GLGarbageBin<GLResource> myGLGarbageBin;

   // More control over blending
   public static enum BlendFactor {
      GL_ONE_MINUS_CONSTANT_ALPHA(GL2GL3.GL_ONE_MINUS_CONSTANT_ALPHA),
      GL_ONE_MINUS_SRC_ALPHA(GL.GL_ONE_MINUS_SRC_ALPHA),
      GL_ONE(GL.GL_ONE),
      GL_ZERO(GL.GL_ZERO),
      GL_SRC_ALPHA(GL.GL_SRC_ALPHA),
      ;

      private int myValue;
      private BlendFactor(int val) {
         myValue = val;
      }
      public int glValue() {
         return myValue;
      }
   }

   public static BlendFactor DEFAULT_SRC_BLENDING = BlendFactor.GL_SRC_ALPHA;
   public static BlendFactor DEFAULT_DST_BLENDING = BlendFactor.GL_ONE_MINUS_CONSTANT_ALPHA;

   // matrices
   protected Matrix4d pickMatrix;
   protected Matrix4d projectionMatrix;
   protected RigidTransform3d viewMatrix;
   protected AffineTransform3dBase modelMatrix;
   protected Matrix3d modelNormalMatrix;            // inverse-transform (for normals)
   protected AffineTransform2dBase textureMatrix;   // transforming texture coordinates
   protected boolean modelMatrixValidP = false;
   protected boolean viewMatrixValidP = false;
   protected boolean projectionMatrixValidP = false;
   protected boolean textureMatrixValidP = false;
   // stacks 
   private LinkedList<Matrix4d> projectionMatrixStack;
   private LinkedList<RigidTransform3d> viewMatrixStack;
   private LinkedList<AffineTransform3dBase> modelMatrixStack;
   private LinkedList<Matrix3d> modelNormalMatrixStack;   // linked to model matrix
   private LinkedList<AffineTransform2dBase> textureMatrixStack;

   protected Vector3d zDir = new Vector3d();     // used for determining zOrder

   protected static final float DEFAULT_DEPTH_OFFSET_INTERVAL = 1e-5f; // prevent z-fighting
   private static final Point3d DEFAULT_VIEWER_CENTER = new Point3d();
   private static final Point3d DEFAULT_VIEWER_EYE = new Point3d (0, -1, 0);

   protected static int DEFAULT_SURFACE_RESOLUTION = 32;
   protected int mySurfaceResolution = DEFAULT_SURFACE_RESOLUTION; 

   private static final float DEFAULT_POINT_SIZE = 1f;
   private static final float DEFAULT_LINE_WIDTH = 1f;
   private static final Shading DEFAULT_SHADING = Shading.FLAT;
   private static final FaceStyle DEFAULT_FACE_STYLE = FaceStyle.FRONT;
   private static final ColorMixing DEFAULT_COLOR_MIXING = ColorMixing.REPLACE;
   private static final ColorInterpolation DEFAULT_COLOR_INTERPOLATION =
      ColorInterpolation.RGB;
   private static final int DEFAULT_DEPTH_OFFSET = 0;

   // viewer state
   protected static class ViewState {
      protected Point3d myCenter = new Point3d (DEFAULT_VIEWER_CENTER);
      protected Point3d myUp = new Point3d (0, 0, 1);

      public ViewState clone() {
         ViewState vs = new ViewState();
         vs.myCenter.set(myCenter);
         vs.myUp.set(myUp);
         return vs;
      }
   }

   protected ViewState myViewState = null;
   protected LinkedList<ViewState> viewStateStack = null;
   protected ViewerState myCommittedViewerState = null;    // "committed" viewer state
   
   private static final int DEFAULT_MULTISAMPLES = 8;

   // Bits to indicate when state variables have been set to non-default values
   static private final int BACK_COLOR_BIT = 0x0001;
   static private final int EMISSION_BIT = 0x0002;
   static private final int SPECULAR_BIT = 0x0004;
   static private final int SHININESS_BIT = 0x0008;
   static private final int COLOR_INTERPOLATION_BIT = 0x0010;
   static private final int HIGHLIGHTING_BIT = 0x0020;
   
   static private final int FACE_STYLE_BIT = 0x0100;
   static private final int LINE_WIDTH_BIT = 0x0200;
   static private final int POINT_SIZE_BIT = 0x0400;
   static private final int SHADING_BIT = 0x0800;
   // surface resolution is not currently restored
   static private final int SURFACE_RESOLUTION_BIT = 0x1000;
   static private final int COLOR_MIXING_BIT = 0x2000;
   static private final int DEPTH_OFFSET_BIT = 0x4000;

   // Status words to record when state variables have been touched or set to
   // non-default values.
   boolean myMappingsSet = false; // need to set
   boolean myModelMatrixSet = false;
   int myNonDefaultColorSettings = 0;
   int myNonDefaultGeneralSettings = 0;

   protected static class ViewerState {
      public boolean lightingEnabled;       // light equations
      public boolean depthEnabled;          // depth buffer
      public boolean depthWriteEnabled;     // depth writes
      public boolean colorEnabled;          // color buffer
      public boolean vertexColorsEnabled;   // use per-vertex colors
      public boolean textureMappingEnabled; // use texture maps
      public FaceStyle faceMode;
      public Shading shading;
      public boolean hsvInterpolationEnabled;  
      public ColorMixing colorMixing;       // method for combining material/vertex colors
      public boolean roundedPoints;
      public boolean blendingEnabled;       // blending
      public BlendFactor blendSFactor;
      public BlendFactor blendDFactor;
      public boolean multiSampleEnabled;    // multi-sampling
      public boolean transparencyEnabled;
      public boolean transparencyFaceCulling;
      public float pointSize;
      public float lineWidth;
      
      public ViewerState() {
         setDefaults();
      }
      
      public void setDefaults() {
         lightingEnabled = true;
         depthEnabled = true;
         depthWriteEnabled = true;
         colorEnabled = true;
         vertexColorsEnabled = true;
         textureMappingEnabled = true;
         faceMode = FaceStyle.FRONT;
         shading = Shading.SMOOTH;
         hsvInterpolationEnabled = false;
         colorMixing = ColorMixing.REPLACE;
         roundedPoints = true;
         blendingEnabled = false;
         blendSFactor = DEFAULT_SRC_BLENDING;
         blendDFactor = DEFAULT_DST_BLENDING;
         transparencyEnabled = false;
         transparencyFaceCulling = false;
         multiSampleEnabled = true;
         pointSize = 1;
         lineWidth = 1;
      }

      public ViewerState clone() {
         ViewerState c = new ViewerState();
         c.lightingEnabled = lightingEnabled;
         c.depthEnabled = depthEnabled;
         c.depthWriteEnabled = depthWriteEnabled;
         c.colorEnabled = colorEnabled;
         c.faceMode = faceMode;
         c.shading = shading;
         c.vertexColorsEnabled = vertexColorsEnabled;
         c.hsvInterpolationEnabled = hsvInterpolationEnabled;
         c.textureMappingEnabled = textureMappingEnabled;
         c.colorMixing = colorMixing;
         c.roundedPoints = roundedPoints;
         c.blendingEnabled = blendingEnabled;
         c.blendSFactor = blendSFactor;
         c.blendDFactor = blendDFactor;
         c.transparencyEnabled = transparencyEnabled;
         c.transparencyFaceCulling = transparencyFaceCulling;
         c.pointSize = pointSize;
         c.lineWidth = lineWidth;
         c.multiSampleEnabled = multiSampleEnabled;
         return c;
      }
   }
   protected ViewerState myViewerState;
   protected LinkedList<ViewerState> viewerStateStack;

   // state requests in case glContext not current
   boolean gammaCorrectionRequested = false;
   boolean gammaCorrectionEnabled = false;

   // frustum parameters
   protected static class ProjectionFrustrum {
      public double near = 1;
      public double far = 1000;
      public double left = -0.5;
      public double right = 0.5;
      public double top = 0.5;
      public double bottom = -0.5;

      public int depthBitOffset = 0;
      public int depthBits = 16;
      public double fov = 30;         // originally 70
      public double fieldHeight = 10; // originally 10
      public boolean orthographic = false;
      public boolean explicit = false;

      public ProjectionFrustrum clone() {
         ProjectionFrustrum c = new ProjectionFrustrum();
         c.near = near;
         c.far = far;
         c.left = left;
         c.right = right;
         c.top = top;
         c.bottom = bottom;
         c.depthBits = depthBits;
         c.depthBitOffset = depthBitOffset;
         c.fov = fov;
         c.fieldHeight = fieldHeight;
         c.orthographic = orthographic;
         c.explicit = explicit;
         return c;
      }
   }
   protected ProjectionFrustrum myFrustum = null;
   LinkedList<ProjectionFrustrum> frustrumStack = null;

   protected boolean resetViewVolume = false;  // adjusting view volume based on screen width/height
   protected boolean resetViewport = false;

   public static final double AUTO_FIT = -1.0; // generic value to trigger an auto-fit

   // View transformations
   protected static final AxisAlignedRotation DEFAULT_AXIAL_VIEW =
      AxisAlignedRotation.X_Z;

   protected AxisAlignedRotation myDefaultAxialView = DEFAULT_AXIAL_VIEW;
   protected AxisAlignedRotation myAxialView = DEFAULT_AXIAL_VIEW;

   public enum RotationMode {
      DEFAULT, CONTINUOUS;
   }
   public static RotationMode DEFAULT_ROTATION_MODE = RotationMode.DEFAULT;
   protected RotationMode myRotationMode = DEFAULT_ROTATION_MODE;

   // enable or disable viewier re-scaling (disable when taking movie)
   protected boolean autoResizeEnabled = true;
   protected boolean autoViewportEnabled = true;

   // program info
   protected GLLightManager lightManager = null;         
   protected GLProgramInfo myProgramInfo = null;    // controls for program to use
   
   // Colors
   protected static final Color DARK_RED = new Color (0.5f, 0, 0);
   protected static final Color DARK_GREEN = new Color (0, 0.5f, 0);
   protected static final Color DARK_BLUE = new Color (0, 0, 0.5f);
   
   protected float[] DEFAULT_MATERIAL_COLOR =    {0.8f, 0.8f, 0.8f, 1.0f};
   protected float[] DEFAULT_MATERIAL_EMISSION = {0.0f, 0.0f, 0.0f, 1.0f};
   protected float[] DEFAULT_MATERIAL_SPECULAR = {0.1f, 0.1f, 0.1f, 1.0f};
   protected float[] DEFAULT_HIGHLIGHT_COLOR =   {1f, 1f, 0f, 1f};
   protected float[] DEFAULT_SELECTING_COLOR =   {0f, 0f, 0f, 0f};
   protected float[] DEFAULT_BACKGROUND_COLOR =  {0f, 0f, 0f, 1f};
   protected float DEFAULT_MATERIAL_SHININESS = 32f;
   
   protected float[] myHighlightColor = Arrays.copyOf (DEFAULT_HIGHLIGHT_COLOR, 4);
   protected boolean myHighlightColorModified = true;
   protected HighlightStyle myHighlightStyle = HighlightStyle.COLOR;
   
   protected float[] mySelectingColor = Arrays.copyOf (DEFAULT_SELECTING_COLOR, 4); // color to use when selecting (color selection)
   protected boolean mySelectingColorModified = true;
   
   protected Material myCurrentMaterial = Material.createDiffuse(DEFAULT_MATERIAL_COLOR, 32f);
   protected float[] myBackColor = null;
   protected boolean myCurrentMaterialModified = true;  // trigger for detecting when material is updated
   protected float[] backgroundColor = Arrays.copyOf (DEFAULT_BACKGROUND_COLOR, 4);
   
   protected static enum ActiveColor {
      DEFAULT,
      HIGHLIGHT,
      SELECTING
   }
   protected ActiveColor myActiveColor = ActiveColor.DEFAULT;     // which front color is currently active
   protected ActiveColor myCommittedColor = null;  // color actually pushed to GPU
   
   // texture properties
   protected ColorMapProps myColorMapProps = null;
   protected NormalMapProps myNormalMapProps = null;
   protected BumpMapProps myBumpMapProps = null;
   
   // Canvas
   protected GLAutoDrawable drawable;  // currently active drawable
   protected GLDrawableComponent canvas;          // main GL canvas
   
   protected int width;
   protected int height;

   // Generic Rendering
   /**
    * Class to set and hold rendering flags.  We enclose these in a class so
    * that we can access them in a synchronized way.
    */
   protected class RenderFlags {
      int myDefaultFlags = 0;
      int mySpecialFlags = 0;
      boolean mySpecialSet = false;

      /**
       * Accessed by application to set default rendering flags.
       */
      public void setDefault (int flags) {
         myDefaultFlags = flags;
      }

      /**
       * Accessed by application to request special flags for the next
       * render. Synchronized access is needed to set multiple values.
       */
      public synchronized void setSpecial (int flags) {
         // 
         mySpecialFlags = flags;
         mySpecialSet = true;
      }

      /**
       * Accessed by rendering code to get the appropriate flags for the
       * current cycle. Synchronized access needed to check and get two values.
       */
      public synchronized int get() {
         if (mySpecialSet) {
            mySpecialSet = false;
            return mySpecialFlags;
         }
         else {
            return myDefaultFlags;
         }
      }

      public boolean isSpecialSet() {
         return mySpecialSet;
      }
   }
   protected RenderFlags myRenderFlags = new RenderFlags();

   // list of renderables
   protected LinkedList<IsRenderable> myRenderables =  new LinkedList<IsRenderable>();
   protected boolean myInternalRenderListValid = false;
   protected RenderList myInternalRenderList = new RenderList();
   protected RenderList myExternalRenderList = null;

   // Renderable Objects and Tools
   protected LinkedList<Dragger3d> myDraggers;
   protected LinkedList<Dragger3d> myUserDraggers;
   protected MouseRayEvent myDraggerSelectionEvent;
   protected Dragger3d myDrawTool;
   protected Object myDrawToolSyncObject = new Object();
   protected Rectangle myDragBox;
   protected GLGridPlane myGrid;
   
   protected double axisLength = 0;
   protected static final boolean DEFAULT_SOLID_AXES = false;
   protected boolean solidAxes = DEFAULT_SOLID_AXES;
   protected boolean gridVisible = false;

   // Cut planes
   protected int maxClipPlanes = 6;                      // minimum 6 supported
   protected ArrayList<GLClipPlane> myClipPlanes = new ArrayList<GLClipPlane>(6);
   protected double[] myClipPlaneValues = new double[4]; // storing plane info

   // Interaction
   protected LinkedList<MouseInputListener> myMouseInputListeners = new LinkedList<MouseInputListener>();
   protected LinkedList<MouseWheelListener> myMouseWheelListeners = new LinkedList<MouseWheelListener>();
   protected GLMouseListener myMouseHandler;
   protected ArrayList<RenderListener> myRenderListeners =
      new ArrayList<RenderListener>();
   public static Vector2d DEFAULT_ELLIPTIC_CURSOR_SIZE = new Vector2d(10,10);
   protected Vector2d myEllipticCursorSize = 
      new Vector2d(DEFAULT_ELLIPTIC_CURSOR_SIZE);
   public static boolean DEFAULT_ELLIPTIC_CURSOR_ACTIVE = false;
   protected boolean myEllipticCursorActive = DEFAULT_ELLIPTIC_CURSOR_ACTIVE;
   private static final boolean DEFAULT_VIEW_ROTATION_ENABLED = true;
   protected boolean myViewRotationEnabled = DEFAULT_VIEW_ROTATION_ENABLED;
   
   // Selection
   protected GLSelector mySelector;
   protected ViewerSelectionFilter mySelectionFilter = null;
   ViewerSelectionEvent selectionEvent;
   protected ArrayList<ViewerSelectionListener> mySelectionListeners = new ArrayList<ViewerSelectionListener>();
   protected boolean selectionEnabled = true;
   protected boolean selectOnPressP = false; 
   protected boolean ellipticSelectionP = false;

   protected boolean myProfiling = false;
   protected FunctionTimer myTimer = new FunctionTimer();

   public static PropertyList myProps = new PropertyList (GLViewer.class);

   static {
      myProps.add (
         "eye", "eye location (world coordinates)", DEFAULT_VIEWER_EYE);
      myProps.add ("center", "center location (world coordinates)", DEFAULT_VIEWER_CENTER);
      myProps.add ("axisLength", "length of rendered x-y-z axes", 0);
      myProps.add (
         "solidAxes", "use solid arrows for rendering axes", DEFAULT_SOLID_AXES);
      myProps.add (
         "rotationMode", "method for interactive rotation", DEFAULT_ROTATION_MODE);
      myProps.add("axialView", "axis-aligned view orientation", DEFAULT_AXIAL_VIEW);
      myProps.add("defaultAxialView", "default axis-aligned view orientation", DEFAULT_AXIAL_VIEW);
      myProps.add ("backgroundColor", "background color", Color.BLACK);
      myProps.add("transparencyFaceCulling", "allow transparency face culling", false);
      myProps.add("blending isBlendingEnabled setBlendingEnabled", "enable/disable blending", false);
      myProps.add(
            "blendSourceFactor", "source transparency blending", DEFAULT_SRC_BLENDING);
         myProps.add(
            "blendDestFactor", "destination transparency blending", DEFAULT_DST_BLENDING);
      myProps.add("surfaceResolution", "resolution for built-in curved primitives", 
         DEFAULT_SURFACE_RESOLUTION);
      myProps.add(
         "profiling", "print timing for render operations", false);
      myProps.add(
         "ellipticCursorActive", "true if the elliptic cursor is active", 
         DEFAULT_ELLIPTIC_CURSOR_ACTIVE);
      myProps.add(
         "ellipticCursorSize", "dimension of the elliptic cursor", 
         DEFAULT_ELLIPTIC_CURSOR_SIZE);
      myProps.add(
         "viewRotationEnabled isViewRotationEnabled *",
         "viewpoint can be rotated in the GUI", 
         DEFAULT_VIEW_ROTATION_ENABLED);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   /**
    * {@inheritDoc}
    */
   public Property getProperty (String name) {
      return PropertyList.getProperty (name, this);
   }

   public void setProfiling (boolean enable) {
      myProfiling = enable;
   }

   public boolean getProfiling() {
      return myProfiling;
   }

   public Vector2d getEllipticCursorSize() {
      return myEllipticCursorSize;
   }
   
   public void setEllipticCursorSize (Vector2d size) {
      myEllipticCursorSize.set (size);
   }
   
   public void resetEllipticCursorSize () {
      myEllipticCursorSize.set (DEFAULT_ELLIPTIC_CURSOR_SIZE);
   }

   public boolean isViewRotationEnabled() {
      return myViewRotationEnabled;
   }
   
   public void setViewRotationEnabled (boolean enable) {
      myViewRotationEnabled = enable;
   }
   
   public boolean getEllipticCursorActive() {
      return myEllipticCursorActive;
   }
   
   public void setEllipticCursorActive (boolean active) {
      myEllipticCursorActive = active;
   }
   
   public int getSurfaceResolution () {
      return mySurfaceResolution;
   }
   
   public int setSurfaceResolution (int nres) {
      int prev = mySurfaceResolution;
      mySurfaceResolution = nres;
//      if (nres != DEFAULT_SURFACE_RESOLUTION) {
//         myNonDefaultGeneralSettings |= SURFACE_RESOLUTION_BIT;
//      }
//      else {
//         myNonDefaultGeneralSettings &= ~SURFACE_RESOLUTION_BIT;
//      }
      return prev;
   }
   
   public void setAxisLength (double len) {
      if (len == AUTO_FIT) {
         Point3d pmin = new Point3d();
         Point3d pmax = new Point3d();
         getBounds (pmin, pmax);
         Vector3d vdiag = new Vector3d();
         vdiag.sub (pmax, pmin);
         axisLength = vdiag.norm() / 2;
      }
      else {
         axisLength = len;
      }
   }

   public double getAxisLength() {
      return axisLength;
   }

   public void setSolidAxes (boolean enable) {
      solidAxes = enable;
   }

   public boolean getSolidAxes() {
      return solidAxes;
   }

   public void setGridVisible (boolean visible) {
      gridVisible = visible;
   }

   public boolean getGridVisible() {
      return gridVisible;
   }

   public KeyListener[] getKeyListeners() {
      return canvas.getKeyListeners();
   }

   public void addKeyListener (KeyListener l) {
      canvas.addKeyListener(l);
   }

   public void removeKeyListener (KeyListener l) {
      canvas.removeKeyListener(l);
   }

   public LinkedList<Dragger3d> getDraggers() {
      // return all draggers except the internal clip plane dragger
      LinkedList<Dragger3d> list = new LinkedList<Dragger3d>();
      list.addAll (myUserDraggers);
      return list;
   }

   public void addSelectionListener (ViewerSelectionListener l) {
      mySelectionListeners.add (l);
   }

   public boolean removeSelectionListener (ViewerSelectionListener l) {
      return mySelectionListeners.remove (l);
   }

   public ViewerSelectionListener[] getSelectionListeners() {
      return mySelectionListeners.toArray (new ViewerSelectionListener[0]);
   }

   public ViewerSelectionEvent getSelectionEvent() {
      return selectionEvent;
   }

   protected void setSelected(List<LinkedList<?>> objs) {
      selectionEvent.setSelectedObjects (objs);
   }

   public void addRenderListener (RenderListener l) {
      myRenderListeners.add (l);
   }

   protected void fireRerenderListeners() {
      if (myRenderListeners.size() > 0) {
         RendererEvent e = new RendererEvent (this);
         for (RenderListener l : myRenderListeners) {
            l.renderOccurred (e);
         }
      }
   }

   public boolean removeRenderListener (RenderListener l) {
      return myRenderListeners.remove (l);
   }

   public RenderListener[] getRenderListeners() {
      return myRenderListeners.toArray (new RenderListener[0]);
   }

   public void addRenderable (IsRenderable d) {
      synchronized(myRenderables) {
         myRenderables.add (d);
      }
      myInternalRenderListValid = false;
   }

   public void addDragger (Dragger3d d) {
      synchronized(myDraggers) {
         myDraggers.add (d);
         myUserDraggers.add (d);
         if (d instanceof Dragger3dBase) {
            ((Dragger3dBase)d).setViewer (this);
         }
      }
      myInternalRenderListValid = false;
   }

   public void setDrawTool (Dragger3d d) {
      synchronized(myDrawToolSyncObject) {
         if (myDrawTool != d) {
            if (myDrawTool instanceof DrawToolBase) {
               ((DrawToolBase)myDrawTool).setViewer (null);
            }
            if (d instanceof DrawToolBase) {
               ((DrawToolBase)d).setViewer (this);
            }
            myDrawTool = d;
         }
      }
   }

   public boolean removeRenderable (IsRenderable d) {
      boolean wasRemoved = false;
      synchronized(myRenderables) {
         wasRemoved = myRenderables.remove (d);
      }
      myInternalRenderListValid = false;
      return wasRemoved;
   }

   public void removeDragger (Dragger3d d) {
      synchronized(myDraggers) {
         if (d instanceof Dragger3dBase) {
            ((Dragger3dBase)d).setViewer (null);
         }
         myDraggers.remove (d);
         myUserDraggers.remove (d);
      }
      myInternalRenderListValid = false;
   }

   public void clearDraggers() {
      synchronized(myDraggers) {
         for (Dragger3d d : myUserDraggers) {
            if (d instanceof Dragger3dBase) {
               ((Dragger3dBase)d).setViewer (null);
            }
            myDraggers.remove (d);
         }
         myUserDraggers.clear();
      }
      myInternalRenderListValid = false;
   }

   public void clearRenderables() {
      synchronized(myRenderables) {
         myRenderables.clear();
      }
      myInternalRenderListValid = false;
   }

   public double getViewPlaneDistance() {
      return myFrustum.near;
   }

   public double getFarPlaneDistance() {
      return myFrustum.far;
   }

   /**
    * Returns the default vertical field of view in degrees. This is used by
    * {@link #autoFitOrtho autoFitOrtho}.
    * 
    * @return default vertical field of view.
    */
   /*
    * This was removed from the above documentation because autoFitView looks
    * like it was removed {@link #autoFitView autoFitView} and
    */
   public double getVerticalFieldOfView() {
      return myFrustum.fov;
   }

   /**
    * Sets the default vertical field of view in degrees. This is used by
    * {@link #autoFitOrtho autoFitOrtho}.
    * 
    * @param fov
    * vertical field of view (degrees).
    */
   /*
    * This was removed from the above documentation because autoFitView looks
    * like it was removed {@link #autoFitView autoFitView} and
    */
   public void setVerticalFieldOfView (double fov) {
      myFrustum.fov = fov;
      computeProjectionMatrix ();
   }

   // for triggering selection process
   protected volatile boolean selectEnabled = false;
   protected volatile boolean selectTrigger = false;

   /**
    * Performs a selection operation on a sub-region of the viewport.
    * 
    * @param x
    * x coordinate of the selection region center
    * @param y
    * y coordinate of the selection region center
    * @param w
    * selection region width
    * @param h
    * selection region height
    * @param ignoreDepthTest
    * select all objects in the pick frustum, not just those which are
    * visible through the viewport
    */
   protected void setPick (
      double x, double y, double w, double h, boolean ignoreDepthTest) {
      if (ignoreDepthTest) {
         if (mySelector == null || mySelector.getClass () != GLOcclusionSelector.class) {
            trash(mySelector);
            mySelector = new GLOcclusionSelector(this);
         }
      }
      else {
         if (mySelector == null || mySelector.getClass () != GLColorSelector.class) {
            trash (mySelector);
            mySelector = new GLColorSelector(this);
         }
      }
      mySelector.setRectangle (x, y, w, h);
      selectTrigger = true;
      repaint();
   }
   
   public void repaint() {
      
      // System.out.println ("GLViewer repaint() called, selectTrigger = " + selectTrigger); 
      
      if (!myInternalRenderListValid) {
         buildInternalRenderList();
      }
      
      if (canvas.isVisible()) {
         canvas.repaint();
      }

   }

   public void paint() {
      canvas.paint (canvas.getGraphics());
   }

   // not currently used
   protected void detachFromCanvas() {
      canvas.removeGLEventListener(this);
   }

   protected void buildInternalRenderList() {
      synchronized(myInternalRenderList) {
         synchronized (myRenderables) {
            myInternalRenderList.clear();
            myInternalRenderList.addIfVisibleAll (myRenderables);
            myInternalRenderListValid = true;
            // myInternalRenderList.addIfVisibleAll (myDraggers);  
         }
      }
   }

   public void setExternalRenderList (RenderList list) {
      myExternalRenderList = list;  
   }

   public RenderList getExternalRenderList() {
      return myExternalRenderList;
   }

   /**
    * Request a render with special flags that will be used
    * only for the duration of that render.
    */
   public void rerender(int flags) {
      buildInternalRenderList();
      myRenderFlags.setSpecial (flags);
      repaint();
   }

   /**
    * Used to see if rendering with special flags has been performed yet.
    */
   public boolean isSpecialRenderFlagsSet() {
      return myRenderFlags.isSpecialSet();
   }

   public void rerender() {
      buildInternalRenderList();
      repaint();
   }

   protected boolean isVisible() {
      return canvas.isVisible();
   }

   /**
    * Returns the bounds for the current frustum. These consist of the
    * quantities left, right, bottom, and top that describe the edge locations
    * of the near clipping plane (in eye coordinates), and near and far, which
    * describe the positions of the near and far clipping planes along the -z
    * axis (again in eye coordinates).
    * 
    * @param bounds
    * returns the values of left, right, bottom, top, near and far (in that
    * order)
    */
   public void getFrustum (double[] bounds) {
      if (bounds.length < 6) {
         throw new IllegalArgumentException (
         "bounds needs a length of at least 6");
      }
      bounds[0] = myFrustum.left;
      bounds[1] = myFrustum.right;
      bounds[2] = myFrustum.bottom;
      bounds[3] = myFrustum.top;
      bounds[4] = myFrustum.near;
      bounds[5] = myFrustum.far;
   }

   /**
    * Sets the viewing frustum to a general perspective projection.
    * 
    * @param left
    * left edge position of the near clipping plane
    * @param right
    * right edge position of the near clipping plane
    * @param bottom
    * bottom edge position of the near clipping plane
    * @param top
    * top position of the near clipping plane
    * @param near
    * near clipping plane position (along the -z axis; must be a positive
    * number)
    * @param far
    * far clipping plane position (along the -z axis; must be a positive number)
    */
   public void setPerspective (
      double left, double right, double bottom, double top, 
      double near, double far) {
      setPerspective(left,  right, bottom, top, near, far, true);
   }

   public void setPerspective (
      double left, double right, double bottom, double top, 
      double near, double far, boolean setExplicit) {
      this.myFrustum.left = left;
      this.myFrustum.right = right;
      this.myFrustum.bottom = bottom;
      this.myFrustum.top = top;
      this.myFrustum.near = near;
      this.myFrustum.far = far;
      this.myFrustum.explicit = setExplicit;
      this.myFrustum.orthographic = false;

      computeProjectionMatrix ();
   }

   /**
    * Sets the viewing frustum to a perspective projection centered about the -z
    * axis. Also sets the default field of view returned by
    * {@link #getVerticalFieldOfView getVerticalFieldOfView}.
    * 
    * @param fieldOfView
    * vertial field of view (in degrees)
    * @param near
    * near clipping plane position (along the -z axis; must be a positive
    * number)
    * @param far
    * far clipping plane position (along the -z axis; must be a positive number)
    */
   public void setPerspective (double fieldOfView, double near, double far) {
      double aspect = width / (double)height;

      this.myFrustum.top = near * Math.tan (Math.toRadians (fieldOfView) / 2);
      this.myFrustum.bottom = -this.myFrustum.top;
      this.myFrustum.left = -aspect * myFrustum.top;
      this.myFrustum.right = -aspect * myFrustum.bottom;
      this.myFrustum.near = near;
      this.myFrustum.far = far;

      this.myFrustum.fov = fieldOfView;
      this.myFrustum.explicit = false;
      this.myFrustum.orthographic = false;

      computeProjectionMatrix ();
   }

   /**
    * Sets the viewing frustum to an orthogonal projection centered about the -z
    * axis.
    * 
    * @param fieldHeight
    * vertical height of the field of view
    * @param near
    * near clipping plane position (along the -z axis; must be a positive
    * number)
    * @param far
    * far clipping plane position (along the -z axis; must be a positive number)
    */
   public void setOrthogonal (double fieldHeight, double near, double far) {
      double aspect = width / (double)height;

      this.myFrustum.top = fieldHeight / 2;
      this.myFrustum.bottom = -this.myFrustum.top;
      this.myFrustum.left = -aspect * myFrustum.top;
      this.myFrustum.right = -aspect * myFrustum.bottom;
      this.myFrustum.near = near;
      this.myFrustum.far = far;
      this.myFrustum.fieldHeight = fieldHeight;
      this.myFrustum.orthographic = true;

      computeProjectionMatrix ();
   }

   /**
    * Sets the viewing frustum to an orthogonal projection centered about the -z
    * axis.
    * 
    * @param fieldHeight
    * vertical height of the field of view
    * @param near
    * near clipping plane position (along the -z axis; must be a positive
    * number)
    * @param far
    * far clipping plane position (along the -z axis; must be a positive number)
    */
   protected void setOrthogonal2d (
      double left, double right, double bottom, double top) {

      setOrthogonal (left, right, bottom, top, -1, 1);
   }
 
   public void setOrthogonal (
      double left, double right, double bottom, double top, 
      double near, double far) {

      this.myFrustum.top = top;
      this.myFrustum.bottom = bottom;
      this.myFrustum.left = left;
      this.myFrustum.right = right;
      this.myFrustum.near = near;
      this.myFrustum.far = far;
      this.myFrustum.orthographic = true;
      this.myFrustum.fieldHeight = myFrustum.top-myFrustum.bottom;

      computeProjectionMatrix ();

   }

   /**
    * Returns true if the current viewing projection is orthogonal.
    * 
    * @return true if viewing projection is orthogonal
    */
   public boolean isOrthogonal() {
      return myFrustum.orthographic;
   }

   public void setOrthographicView (boolean enable) {
      if (enable) {
         autoFitOrtho();
      }
      else {
         autoFitPerspective();
      }
   }

   public boolean getBounds (Point3d pmin, Point3d pmax) {
      for (int i = 0; i < 3; i++) {
         pmin.set (i, Double.POSITIVE_INFINITY);
         pmax.set (i, Double.NEGATIVE_INFINITY);
      }
      boolean boundsSet = false;
      for (IsRenderable renderable : myRenderables) {
         renderable.updateBounds (pmin, pmax);
         boundsSet = true;
      }
      RenderList elist = myExternalRenderList;
      if (elist != null) {
         elist.updateBounds (pmin, pmax);
         boundsSet = true;
      }
      if (!boundsSet) {
         for (IsRenderable renderable : myDraggers) {
            renderable.updateBounds (pmin, pmax);
         }
      }
      // System.out.println (pmin);
      // System.out.println (pmax);
      if (pmin.x == Double.POSITIVE_INFINITY) { // then no bounds were set, so
         // use a default
         pmin.set (-1, -1, -1);
         pmax.set (1, 1, 1);
         return false;
      }
      else {
         return true;
      }
   }

   /**
    * Size is the diameter of the bounding box.
    */
   public double estimateRadiusAndCenter (Point3d center) {
      Point3d pmin = new Point3d();
      Point3d pmax = new Point3d();

      getBounds (pmin, pmax);
      if (center != null) {
         center.add (pmin, pmax);
         center.scale (0.5);
      }

      Vector3d vdiag = new Vector3d();
      vdiag.sub (pmax, pmin);
      double r = vdiag.norm() / 2;
      return r;
   }

   public void autoFit() {
      if (isOrthogonal()) {
         autoFitOrtho ();
      }
      else {
         autoFitPerspective (); // check if size is affected via autofit
      }
   }

   private boolean hasRenderables() {
      RenderList elist = myExternalRenderList;
      return (
      myRenderables.size() > 0 ||
      (elist != null && elist.size() > 0) ||
      myDraggers.size() > 0 || 
      myDrawTool != null);
   }

   public void setGridSizeAndPosition (Point3d pcenter, double r) {

      myGrid.setMinSize (4 * r);
      myGrid.setAutoSized (true);
      myGrid.setPosition (pcenter);
   }

   /**
    * {@inheritDoc}
    */
   public void autoFitPerspective () {
      if (hasRenderables()) {
         Point3d pcenter = new Point3d();
         double r = estimateRadiusAndCenter (pcenter);
         //if radius is zero, set default to radius 1
         if ( Math.abs(r) == 0 || Double.isInfinite(r) || Double.isNaN(r)) {
            r = 1;
         }
         double far = 40 * r;
         double near = far / 1000;

         myViewState.myCenter.set (pcenter);
         Vector3d zdir = getEyeZDirection();
         // use sine instead of tangent since we want frustum to be tangent to
         // to the sphere implied by r
         double d = r / Math.sin (Math.toRadians (myFrustum.fov) / 2);
         Point3d eye = new Point3d();
         eye.scaledAdd(d, zdir, myViewState.myCenter);
         setEye(eye);

         setPerspective (myFrustum.fov, near, far);
         setGridSizeAndPosition (pcenter, r);

         if (isVisible()) {
            rerender();
         }
      }
   }

   /*
    * {@inheritDoc}
    */
   public void autoFitOrtho () {
      if (hasRenderables()) {
         Point3d pcenter = new Point3d();
         double r = estimateRadiusAndCenter (pcenter);

         //if radius is zero, set default to radius 1
         if ( Math.abs(r) == 0  || Double.isInfinite(r) || Double.isNaN(r)) {
            r = 1;
         }

         myViewState.myCenter.set (pcenter);
         Vector3d zdir = getEyeZDirection();
         // use sine instead of tangent since we want frustum to be tangent to
         // to the sphere implied by r
         double d = r / Math.sin (Math.toRadians (myFrustum.fov) / 2);
         Point3d eye = getEye();
         eye.scaledAdd(d, zdir, myViewState.myCenter);
         setEye(eye);

         double far = 40 * r;
         double near = far / 1000;
         setOrthogonal (2 * r, near, far);
         setGridSizeAndPosition (pcenter, r);

         if (isVisible()) {
            rerender();
         }
      }
   }

   public int getScreenWidth() {
      return width;
   }

   public int getScreenHeight() {
      return height;
   }

   public int getScreenX() {
      return canvas.getX();
   }

   public int getScreenY() {
      return canvas.getY();
   }

   public GL getGL() {
      if (drawable != null) {
         return drawable.getGL();
      }
      return null;
   }

   // get the actual line width currently set by GL
   protected float getLineWidth(GL gl) {
      float[] buff = new float[1];
      gl.glGetFloatv(GL.GL_LINE_WIDTH, buff, 0);
      return buff[0];
   }

   // get the actual point size currently set by GL
   protected float getPointSize(GL2GL3 gl) {
      float[] buff = new float[1];
      gl.glGetFloatv(GL.GL_POINT_SIZE, buff, 0);
      return buff[0];
   }
   
   /**
    * Explicitly set the point size now, updating committed
    * state if necessary
    */
   protected void setPointSize(GL2GL3 gl, float size) {
      setPointSize (size);
      if (myCommittedViewerState != null) {
         if (myCommittedViewerState.pointSize != size) {
            gl.glPointSize (size);
            myCommittedViewerState.pointSize = size;
         }
      } else {
         gl.glPointSize (size);
      }
   }
   
   /**
    * Explicitly set the line width now, updating committed
    * state if necessary
    */
   protected void setLineWidth(GL gl, float width) {
      setLineWidth (width);
      if (myCommittedViewerState != null) {
         if (myCommittedViewerState.lineWidth != width) {
            gl.glLineWidth (width);
            myCommittedViewerState.lineWidth = width;
         }
      } else {
         gl.glLineWidth (width);
      }
   }

   /**
    * Enable or disable the GL Canvas auto-swap mode
    */
   public void setAutoSwapBufferMode (boolean enable) {
      canvas.setAutoSwapBufferMode (enable);
   }

   /**
    * Check whether or not the GL canvas is set to auto swap buffers
    */
   public boolean getAutoSwapBufferMode() {
      return canvas.getAutoSwapBufferMode();
   }

   /**
    * Gets the "currently active" context.  If not currently rendering,
    * this will return null;
    * @return active context
    */
   public GLContext getContext() {
      if (drawable != null) {
         return drawable.getContext();
      }
      return null;
   }

 
   //   /**
   //    * Distance of pixel from center (Euchlidean)
   //    * @param x
   //    * @param y
   //    * @return
   //    */
   //   private double centerDistance (int x, int y) {
   //      int dx = x - width / 2;
   //      int dy = y - height / 2;
   //      return Math.sqrt (dx * dx + dy * dy);
   //   }

   /**
    * Rotate the eye coordinate frame about the center point, independent
    * of the default up vector.
    * 
    * @param xang
    * amount of horizontal rotation (in radians)
    * @param yang
    * amount of vertical rotation (in radians)
    */
   protected void rotateContinuous (double xang, double yang) {

      Vector3d reye = new Vector3d();
      reye.sub (getEye(), myViewState.myCenter);

      Vector3d yCam = new Vector3d();     // up-facing vector
      Vector3d xCam = new Vector3d();     // right-facing vector

      synchronized(viewMatrix) {
         viewMatrix.R.getRow(1, yCam);
         viewMatrix.R.getRow(0, xCam);
      }

      //System.out.println("Transform: " + XEyeToWorld.R);
      if (yang != 0) {
         RotationMatrix3d R = new RotationMatrix3d(new AxisAngle(xCam, yang));
         reye.transform(R);
         yCam.transform(R);
      } 
      if (xang != 0) {
         reye.transform(new RotationMatrix3d(new AxisAngle(yCam, xang)));
      }
      Point3d eye = new Point3d();      
      eye.add (reye, myViewState.myCenter);
      setEyeToWorld (eye, myViewState.myCenter, yCam);

      repaint();
   }

   /**
    * Rotate the eye coordinate frame about the center point, while maintaining
    * the default up vector.
    * 
    * @param xang
    * amount of horizontal rotation (in radians)
    * @param yang
    * amount of vertical rotation (in radians)
    * @see #getUpVector
    */
   protected void rotateFixedUp (double xang, double yang) {
      Vector3d reye = new Vector3d();
      reye.sub (getEye(), myViewState.myCenter);

      if (yang != 0) {
         Vector3d xCam = new Vector3d(); // right-facing vector

         synchronized(viewMatrix) {
            viewMatrix.R.getRow (0, xCam);
         }

         double oldAngle = Math.acos (reye.dot (myViewState.myUp) / reye.norm());
         if (!((yang < 0 && (-yang) > oldAngle) ||
         (yang > 0 && yang > (Math.PI - oldAngle)))) {
            reye.transform (new RotationMatrix3d (new AxisAngle (xCam, yang)));
         } 

      }
      if (xang != 0) {
         reye.transform (new RotationMatrix3d (new AxisAngle (myViewState.myUp, xang)));
      }

      Point3d eye = new Point3d();
      eye.add (reye, myViewState.myCenter);

      setEyeToWorld (eye, myViewState.myCenter, myViewState.myUp);

      repaint();
   }

   /**
    * Rotate the eye coordinate frame about the center point
    * 
    * @param xang
    * amount of horizontal rotation (in radians)
    * @param yang
    * amount of vertical rotation (in radians)
    */
   public void rotate (double xang, double yang) {

      switch (myRotationMode) {
         case CONTINUOUS:
            rotateContinuous(xang, yang);
            break;
         default:
            rotateFixedUp(xang, yang);
            break;
      }

   }

   /**
    * Translate the eye position with respect to the x-y plane of the eye frame.
    * The center point is translated by the same amount.
    * 
    * @param delx
    * x translation amount
    * @param dely
    * y translation amount
    */
   protected void translate (double delx, double dely) {
      Vector3d xCam = new Vector3d(), yCam = new Vector3d();

      synchronized (viewMatrix) {
         viewMatrix.R.getRow (0, xCam);
         viewMatrix.R.getRow (1, yCam);  
      }

      Vector3d offset = new Vector3d();
      offset.scale (-delx, xCam);
      offset.scaledAdd (-dely, yCam, offset);

      myViewState.myCenter.add (offset);
      Point3d eye = getEye();
      eye.add(offset);
      setEyeToWorld (eye, myViewState.myCenter, getActualUpVector()); 

      repaint();
   }

   /**
    * Zoom in or out by a specified scale factor. A factor larger than one zooms
    * out, while a factor less than one zooms in. In orthographic projection,
    * zoom is accomplished changing the frustum size. In perspective projection,
    * it is accomplished by moving the eye position along the z axis of the eye
    * frame.
    * 
    * @param s
    * scale factor
    */
   public void zoom (double s) {
      if (myFrustum.orthographic) {
         myFrustum.fieldHeight *= s;
         myFrustum.top *= s;
         myFrustum.bottom *= s;
         myFrustum.left *= s;
         myFrustum.right *= s;
         computeProjectionMatrix ();  // update projection matrix
      }
      else {
         Vector3d reye = new Vector3d();
         Point3d eye = getEye();

         synchronized(viewMatrix) {
            reye.sub (eye, myViewState.myCenter);
            reye.transform(viewMatrix);
            reye.x = reye.y = 0;
            reye.inverseTransform (viewMatrix);
         }
         eye.scaledAdd (s - 1, reye);
         setEyeToWorld (eye, myViewState.myCenter, getActualUpVector());
      }
      repaint();
   }

   /**
    * Computes the distance per pixel for a point specified with respect to
    * world coordinates.
    */
   public double distancePerPixel (Vector3d pnt) {
      if (myFrustum.orthographic) {
         return myFrustum.fieldHeight / height;
      }
      else {
         Point3d pntInEye = new Point3d (pnt);
         pntInEye.transform (viewMatrix);
         return Math.abs (pntInEye.z / myFrustum.near) * (myFrustum.top - myFrustum.bottom) / height;
      }
   }

   /**
    * Computes the distance per pixel at the viewpoint center.
    */
   public double centerDistancePerPixel() {
      return distancePerPixel (myViewState.myCenter);
   }

   private Color getAxisColor (int idx) {
      switch (idx) {
         case 0: {
            return DARK_RED;
         }
         case 1: {
            return DARK_GREEN;
         }
         case 2: {
            return DARK_BLUE;
         }
         default: {
            throw new InternalErrorException ("unknown index "+idx);
         }
      }
   }

   /**
    * Adjusts the orientation the eye frame with respect to world
    * coordinates. The grid is adjusted to align with the nearest set
    * of aligned axes. 
    * The distance between the center and the eye frame is unchanged.
    * 
    * @param REW
    * desired rotational transform from eye to world coordinates
    */
   protected void setAlignedEyeOrientation (RotationMatrix3d REW) {

      Vector3d xdir = new Vector3d();
      Vector3d ydir = new Vector3d();
      Vector3d zdir = new Vector3d();

      REW.getColumn (0, xdir);
      REW.getColumn (1, ydir);
      REW.getColumn (2, zdir);

      // new eye to world transfrom
      RigidTransform3d X = new RigidTransform3d();
      X.R.set (REW);
      double d = getEye().distance (myViewState.myCenter);
      X.p.scaledAdd (d, zdir, myViewState.myCenter);
      myViewState.myUp.set (ydir);
      setEyeToWorld (X);

      X.p.setZero();
      // adjust X.R to the nearest axis-aligned orientation
      int xmaxIdx = xdir.maxAbsIndex();
      double v = xdir.get(xmaxIdx) > 0 ? 1 : -1;
      xdir.setZero();
      xdir.set (xmaxIdx, v);

      ydir.set (xmaxIdx, 0);
      int ymaxIdx = ydir.maxAbsIndex();
      v = ydir.get(ymaxIdx) > 0 ? 1 : -1;
      ydir.setZero();
      ydir.set (ymaxIdx, v);

      X.R.setXYDirections (xdir, ydir);      
      myGrid.setGridToWorld (X);
      myGrid.setUseWorldOrigin (true);
      myGrid.setLockAxesToWorld (true);
      myGrid.setXAxisColor (getAxisColor (0));
      myGrid.setYAxisColor (getAxisColor (1));
      myGrid.setZAxisColor (getAxisColor (2));
   }

   public void setDefaultAxialView (AxisAlignedRotation view) {
      setAxialView (view);
      myDefaultAxialView = view;
   }

   public AxisAlignedRotation getDefaultAxialView () {
      return myDefaultAxialView;
   }

   /**
    * Sets an axial (or axis-aligned) view. This is done by setting the 
    * rotational part of the eye-to-world transform to the axis-aligned
    * rotation <code>REW</code>, and then moving the eye position so that 
    * the center position lies along the new -z axis of the eye frame, 
    * while maintaining the current distance between the eye and the center. 
    * 
    * <p>The method also sets this viewer's `up'' vector to the y axis of 
    * <code>REW</code>, and saves <code>REW</code> itself as the current
    * axis-aligned view, which can be retrieved using {@link #getAxialView}.
    * The viewer's grid is also adjusted to align with the nearest set
    * of aligned axes. 
    * 
    * @param REW axis-aligned rotational component for 
    * the eye-to-world transform
    * @see #getAxialView
    * @see #setUpVector
    * @see #getUpVector
    */
   public void setAxialView (AxisAlignedRotation REW) {
      setAlignedEyeOrientation (REW.getMatrix());
      myAxialView = REW;
   }

   /**
    * {@inheritDoc}
    */
   public AxisAlignedRotation getAxialView() {
      return myAxialView;
   }

   // end of the rotation code

   public GLViewer () {   

      myGLGarbageBin = new GLGarbageBin<> ();
      
      myFrustum = new ProjectionFrustrum();
      frustrumStack = new LinkedList<>();

      myViewState = new ViewState();
      viewStateStack = new LinkedList<>();

      myViewerState = new ViewerState();
      viewerStateStack = new LinkedList<>();

      // initialize matrices
      projectionMatrix = new Matrix4d();
      viewMatrix = new RigidTransform3d();
      modelMatrix = new RigidTransform3d();
      modelNormalMatrix = new Matrix3d(modelMatrix.getMatrix());
      textureMatrix = RigidTransform2d.IDENTITY.copy();

      projectionMatrixStack = new LinkedList<>();
      viewMatrixStack = new LinkedList<>();
      modelMatrixStack = new LinkedList<>();
      modelNormalMatrixStack = new LinkedList<>();
      textureMatrixStack = new LinkedList<> ();

      computeProjectionMatrix ();
      invalidateModelMatrix();
      invalidateViewMatrix();
      invalidateProjectionMatrix();
      invalidateTextureMatrix ();
      
      myProgramInfo = new GLProgramInfo();
   }

   public GLDrawableComponent getCanvas() {
      return canvas;
   }

   /**
    * Called any time GL context is switched! e.g. moving window to new display
    */
   public void init (GLAutoDrawable drawable) {
      GL gl = drawable.getGL ();
      String renderer = gl.glGetString(GL.GL_RENDERER);
      String version = gl.glGetString(GL.GL_VERSION);
      
      Logger logger = Logger.getSystemLogger();
      logger.info("GL Renderer: " + renderer);
      logger.info("OpenGL Version: " + version);
      
      setMultiSampleEnabled (true);
      myActiveColor = ActiveColor.DEFAULT;

   }
   
   public boolean isMultiSampleEnabled() {
      return myViewerState.multiSampleEnabled;
   }
   
   public int numMultiSamples() {
      return DEFAULT_MULTISAMPLES;
   }
   
   public void setMultiSampleEnabled(boolean set) {
      myViewerState.multiSampleEnabled = set;
   }
   
   /**
    * Mark resource for disposal
    * @param resource
    */
   protected void trash(GLResource resource) {
      myGLGarbageBin.trash (resource);
   }
   
   /**
    * Clean garbage by disposing resources
    * @param gl context
    */
   protected void garbage(GL gl) {
      myGLGarbageBin.garbage (gl);
   }

   /**
    * Called any time GL context is switched! e.g. moving window to new display
    */
   public void dispose(GLAutoDrawable drawable) {
      myCommittedViewerState = null;
      
      GL gl = drawable.getGL ();
      if (mySelector != null) {
         mySelector.dispose (gl);
      }
      garbage (gl);
   }
   
   public void addMouseInputListener (MouseInputListener l) {
      if (canvas != null) {
         synchronized(canvas) {
            canvas.addMouseListener (l);
            canvas.addMouseMotionListener (l);
         }
      }
      myMouseInputListeners.add (l);
   }

   public void removeMouseInputListener (MouseInputListener l) {
      if (canvas != null) {
         synchronized(canvas) {
            canvas.removeMouseListener (l);
            canvas.removeMouseMotionListener (l);
         }
      }
      myMouseInputListeners.remove (l);
   }

   public MouseInputListener[] getMouseInputListeners() {
      return myMouseInputListeners.toArray (new MouseInputListener[0]);
   }

   public void addMouseWheelListener (MouseWheelListener l) {
      if (canvas != null) {
         synchronized(canvas) {
            canvas.addMouseWheelListener (l);
         }
      }
      myMouseWheelListeners.add (l);
   }

   public void removeMouseWheelListener (MouseWheelListener l) {
      if (canvas != null) {
         synchronized(canvas) {
            canvas.removeMouseWheelListener (l);
         }
      }
      myMouseWheelListeners.remove (l);
   }

   public MouseWheelListener[] getMouseWheelListeners() {
      return myMouseWheelListeners.toArray (new MouseWheelListener[0]);
   }

   public void reshape (GLAutoDrawable drawable, int x, int y, int w, int h) {
      this.drawable = drawable;
      
      width = w;
      height = h;

      // screen size changed, so be prepared to adjust viewport/view volume
      resetViewVolume = true;
      resetViewport = true;
      repaint();
      
      this.drawable = null;
   }

   public double getViewPlaneHeight() {
      if (myFrustum.orthographic) {
         return myFrustum.fieldHeight;
      }
      else {
         return myFrustum.top - myFrustum.bottom;
      }
   }

   public double getViewPlaneWidth() {
      return (width / (double)height) * getViewPlaneHeight();
   }

   @Override
   public void setPointSize(float s) {
      myViewerState.pointSize = s;
      if (s != DEFAULT_POINT_SIZE) {
         myNonDefaultGeneralSettings |= POINT_SIZE_BIT;
      }
      else {
         myNonDefaultGeneralSettings &= ~POINT_SIZE_BIT;
      }
   }
   
   @Override
   public float getPointSize() {
      return myViewerState.pointSize;
   }

   @Override
   public void setLineWidth(float w) {
      myViewerState.lineWidth = w;
      if (w != DEFAULT_LINE_WIDTH) {
         myNonDefaultGeneralSettings |= LINE_WIDTH_BIT;
      }
      else {
         myNonDefaultGeneralSettings &= ~LINE_WIDTH_BIT;
      }
   }
  
   public float getLineWidth() {
      return myViewerState.lineWidth;
   }
   
   /**
    * Sets the viewer's viewport. 
    */
   public void setViewport(GL gl, int x, int y, int width, int height) {
      gl.glViewport(x, y, width, height);
      // System.out.println (
      //    "viewport set to "+width+"x"+height+", surface "+
      //    canvas.getSurfaceWidth()+"x"+canvas.getSurfaceHeight());         
   }

   protected int[] getViewport(GL gl) {
      int[] buff = new int[4];
      gl.glGetIntegerv(GL.GL_VIEWPORT, buff, 0);
      return buff;
   }

   protected void resetViewVolume(GL gl) {
      resetViewVolume(gl, width, height);
   }

   protected void resetViewVolume(GL gl, int width, int height) {
      if (myFrustum.orthographic) {
         setOrthogonal(myFrustum.fieldHeight, myFrustum.near, myFrustum.far);
      }
      else {
         if (myFrustum.explicit) {
            setPerspective (
               myFrustum.left, myFrustum.right, myFrustum.bottom, myFrustum.top, 
               myFrustum.near, myFrustum.far);
         }
         else {
            double aspect = width / (double)height;
            myFrustum.left = -aspect * myFrustum.top;
            myFrustum.right = -myFrustum.left;
            setPerspective (
               myFrustum.left, myFrustum.right, myFrustum.bottom, myFrustum.top, 
               myFrustum.near, myFrustum.far, myFrustum.explicit);
         }
      }
      setViewport(gl, 0, 0, width, height);
   }

   // Sanchez, July 2013:
   // used to adjust selection volume, or else the orthogonal
   // view scale sometimes too large to properly detect selections
   protected void getZRange(Vector2d zRange) {

      if (!isOrthogonal()) {
         zRange.x = myFrustum.near;
         zRange.y = myFrustum.far;
         return;
      }

      Point3d pmin = new Point3d();
      Point3d pmax = new Point3d();
      getBounds(pmin, pmax);


      Vector3d zdir = getEyeZDirection();

      // John Lloyd, June 2016. Try to get a more robust estimate of the
      // view volume, based on the center and maximum "radius" of the 
      // bounding box

      Point3d cent = new Point3d();
      Vector3d diag = new Vector3d();
      cent.add (pmax, pmin);
      cent.scale (0.5);
      diag.sub (pmax, pmin);

      cent.transform (getViewMatrix());
      double worldDist = -cent.z;
      double radius = diag.norm()/2;
      if (radius == 0) {
         // can happen if model contains only one or more co-located points
         radius = worldDist*0.01;
      }
      // add 20% to radius for good measure
      zRange.y = worldDist + 1.2*radius;
      zRange.x = worldDist - 1.2*radius;

      // // find max z depth
      // double worldDist = Math.abs(getEye().dot(zdir));
      // double [] x = {pmin.x, pmax.x};
      // double [] y = {pmin.y, pmax.y};
      // double [] z = {pmin.z, pmax.z};
      // double minz = Double.POSITIVE_INFINITY;
      // double maxz = Double.NEGATIVE_INFINITY;
      // for (int i=0; i<2; i++) {
      //    for (int j=0; j<2; j++) {
      //       for (int k=0; k<2; k++) {
      //          double d = x[i]*zdir.x + y[j]*zdir.y + z[k]*zdir.z;
      //          maxz = Math.max(maxz, d);
      //          minz = Math.min(minz, d);
      //       }
      //    }
      // }

      // // add 50% for good measure
      // double d = maxz-minz;
      // minz = minz-d/2;
      // maxz = maxz+d/2;
      
      // zRange.y = maxz + worldDist;
      // zRange.x = 2*(minz + worldDist)-zRange.y;
   }

   public void setViewVolume (double near, double far) {
      if (myFrustum.orthographic) {
         setOrthogonal(myFrustum.fieldHeight, near, far);
      }
      else {
         if (myFrustum.explicit) {
            setPerspective(myFrustum.left, myFrustum.right, myFrustum.bottom, myFrustum.top, near, far);
         }
         else {
            double aspect = width / (double)height;
            myFrustum.left = -aspect * myFrustum.top;
            myFrustum.right = -myFrustum.left;
            setPerspective(myFrustum.left, myFrustum.right, myFrustum.bottom, myFrustum.top, near, far, myFrustum.explicit);
         }
      }
   }
   
   public void getViewMatrix (RigidTransform3d TWE) {
      TWE.set(viewMatrix);
   }
   
   public RigidTransform3d getViewMatrix () {
      return viewMatrix.copy();
   }

   public void getEyeToWorld (RigidTransform3d X) {
      X.invert(viewMatrix);
   }

   /**
    * Directly sets the eye coordinate frame.
    * 
    * @param X
    * new EyeToWorld transformation
    */
   public void setEyeToWorld (RigidTransform3d X) {
      viewMatrix.invert(X);
      // XXX
      if (Math.abs (viewMatrix.getOffset ().norm ()) < 1e-5) {
         // System.err.println ("bang"); Thread.dumpStack();
      }
      invalidateViewMatrix();
      repaint();
   }

   /**
    * Sets the eyeToWorld transform for this viewer, using the canonical
    * parameters used by the GL <code>lookat</code> method.
    * 
    * @param eye
    * position of the eye, in world coordinates
    * @param center
    * point that the eye is looking at, in world coordinates
    * @param up
    * up direction, in world coordinates
    */
   public void setEyeToWorld (Point3d eye, Point3d center, Vector3d up) {

      Vector3d zaxis = new Vector3d();
      Vector3d yaxis = new Vector3d();
      Vector3d xaxis = new Vector3d();
      zaxis.sub(eye, center);

      double n = zaxis.norm();
      if (n > 1e-12) {
         zaxis.scale(1.0/n);
      } else {
         RotationMatrix3d R = new RotationMatrix3d();
         R.rotateZDirection(up);
         R.getColumn(0, zaxis);
         R.getColumn(1, xaxis);
         R.getColumn(2, yaxis);
      }

      xaxis.cross(up, zaxis);
      n = xaxis.norm();
      if (n > 1e-6) {
         xaxis.scale(1.0/n);
         yaxis.cross(zaxis, xaxis);
         yaxis.normalize();
      } else {
         RotationMatrix3d R = new RotationMatrix3d();
         R.rotateZDirection(zaxis);
         R.getColumn(1, yaxis);
         R.getColumn(0, xaxis);
      }

      synchronized(viewMatrix) {
         viewMatrix.set(new double[]{
                                     xaxis.x, xaxis.y, xaxis.z, -xaxis.dot(eye),
                                     yaxis.x, yaxis.y, yaxis.z, -yaxis.dot(eye),
                                     zaxis.x, zaxis.y, zaxis.z, -zaxis.dot(eye),
         });
      }
      // XXX
      if (Math.abs (viewMatrix.getOffset ().norm ()) < 1e-5) {
         // System.err.println ("bang"); Thread.dumpStack();
      }

      invalidateViewMatrix();
   }

//   John Lloyd: didn't work well - not enough depth buffer precision
//   close to the far plane
//   /**
//    * Add a depth offset to the projection matrix.
//    * Each integer represents enough depth to account for one bin in the depth
//    * buffer.  Negative values bring following objects closer to the screen.
//    * This is to account for z-fighting.
//    * 
//    * @param zOffset value to offset depth buffer
//    */
//   public void addDepthOffset(double zOffset) {
//      // compute depth buffer precision
//      double deps = 2.0/(1 << (myFrustum.depthBits-1));
//      // Let n and f be the far and near plane distances (positive values),
//      // and z the z value in eye coordinates. Then the change in z 
//      // corresponding to deps is
//      //
//      // delta z = - ((f-n) z^2)/(2 f n) deps
//      //
//      // We take z to be about 1/10 the distance to the far plane, which
//      // corresponds roughly to where the center is when autoFit is used.
//      Vector3d dp = new Vector3d();
//      double f = myFrustum.far;
//      double n = myFrustum.near;
//      dp.scale (zOffset*(f-n)*f*deps/(2*n), getEyeZDirection());
//      synchronized (modelMatrix) {
//         modelMatrix.addTranslation (dp.x, dp.y, dp.z);
//      }
//      invalidateModelMatrix();
//   }
   
//   public void setModelMatrix2d (double width, double height) {
//      setModelMatrix2d (0, width, 0, height);
//   }
 
   /**
    * {@inheritDoc}
    */
   public void setModelMatrix2d (
      double left, double right, double bottom, double top) {
      AffineTransform3d XMW = new AffineTransform3d();
      double w = right-left;
      double h = top-bottom;      
      XMW.A.m00 = 2/w;
      XMW.A.m11 = 2/h;
      XMW.p.set (-(left+right)/w, -(top+bottom)/h, 0);
      setModelMatrix (XMW);
   }

   /**
    * {@inheritDoc}
    */
   public void setDepthOffset(int offset) {
      myFrustum.depthBitOffset = offset;
      if (offset != DEFAULT_DEPTH_OFFSET) {
         myNonDefaultGeneralSettings |= DEPTH_OFFSET_BIT;
      }
      else {
         myNonDefaultGeneralSettings &= ~DEPTH_OFFSET_BIT;
      }      
      computeProjectionMatrix ();
   }

   /**
    * {@inheritDoc}
    */
   public int getDepthOffset() {
      return myFrustum.depthBitOffset;
   }

   public void display (GLAutoDrawable drawable) {
      
      // System.out.println ("GLViewer display(GLAutoDrawable drawable) called with " + drawable.getClass ());
      
      // assign current drawable
      this.drawable = drawable;
      
      // reset attributes due to possible change by GLJPanel
      if (useGLJPanel) {
         myCommittedViewerState = null;
         myCommittedColor = null;      
         myCurrentMaterialModified = true;
      }
      
      int flags = myRenderFlags.get();

      // check if gamma property needs to be changed
      if (gammaCorrectionRequested) {
         GL gl = drawable.getGL ();
         if (gammaCorrectionEnabled) {
            gl.glEnable(GL2GL3.GL_FRAMEBUFFER_SRGB);
         } else {
            gl.glDisable(GL2GL3.GL_FRAMEBUFFER_SRGB);
         }
         gammaCorrectionRequested = false;
      }
      
      int depthBits = drawable.getChosenGLCapabilities().getDepthBits ();
      if (depthBits != myFrustum.depthBits) {
         myFrustum.depthBits = depthBits;
         computeProjectionMatrix ();
      }
      display(drawable, flags);
      
      // clear current drawable
      this.drawable = null;
   }
   
   protected boolean hasTransparent3d() {
      if (myInternalRenderList.numTransparent() > 0) {
         return true;
      }
      RenderList elist = myExternalRenderList;
      if (elist != null) {
         if (elist.numTransparent() > 0) {
            return true;
         }
      }
      return false;
   }

   protected boolean has2d() {
      if (myInternalRenderList.numOpaque2d() > 0 ||
      myInternalRenderList.numTransparent2d() > 0) {
         return true;
      }
      RenderList elist = myExternalRenderList;
      if (elist != null) {
         if (elist.numOpaque2d() > 0 || 
             elist.numTransparent2d() > 0) {
            return true;
         }
      }
      return false;
   }

   protected boolean hasTransparent2d() {
      if (myInternalRenderList.numTransparent2d() > 0) {
         return true;
      }
      RenderList elist = myExternalRenderList;
      if (elist != null) {
         if (elist.numTransparent2d() > 0) {
            return true;
         }
      }
      return false;
   }

   private class RenderIterator implements Iterator<IsRenderable> {

      SortedRenderableList myList = null;
      RenderList myExtList;
      int myListIdx = 0;
      int myIdx = -1;
      final int MAX_LIST_IDX = 7;

      public RenderIterator() {
         myList = myInternalRenderList.getOpaque();
         myExtList = myExternalRenderList;
         myIdx = -1;
         myListIdx = 0;
         advance();
      }

      public boolean hasNext() {
         return myList != null;
      }

      public IsRenderable next() {
         if (myList == null) {
            throw new NoSuchElementException();
         }
         IsRenderable r = myList.get(myIdx);
         advance();
         return r;
      }

      public void remove() {
         throw new UnsupportedOperationException();
      }

      private SortedRenderableList getList (int idx) {
         switch (idx) {
            case 0: {
               return myInternalRenderList.getOpaque();
            }
            case 1: {
               return (myExtList != null ?
                  myExtList.getOpaque() : null);
            }
            case 2: {
               return myInternalRenderList.getTransparent();
            }
            case 3: {
               return (myExtList != null ?
                  myExtList.getTransparent() : null);
            }
            case 4: {
               return myInternalRenderList.getOpaque2d();
            }
            case 5: {
               return (myExtList != null ?
                  myExtList.getOpaque2d() : null);
            }
            case 6: {
               return myInternalRenderList.getTransparent2d();
            }
            case 7: {
               return (myExtList != null ?
                  myExtList.getTransparent2d() : null);
            }
            default: {
               throw new ArrayIndexOutOfBoundsException ("idx=" + idx);
            }
         }
      }               

      private void advance() {
         myIdx++;
         if (myIdx >= myList.size()) {
            myIdx = 0;
            myList = null;
            while (++myListIdx <= MAX_LIST_IDX) {
               SortedRenderableList l = getList (myListIdx);
               if (l != null && l.size() > 0) {
                  myList = l;
                  break;
               }
            }
         }
      }
   }

   protected Iterator<IsRenderable> renderIterator() {
      return new RenderIterator();
   }

   public abstract void display (GLAutoDrawable drawable, int flags);

   public void setBackgroundColor (float r, float g, float b) {
      setBackgroundColor(r, g, b, 1.0f);
   }

   protected void setBackgroundColor(float r, float g, float b, float a) {
      backgroundColor[0] = r;
      backgroundColor[1] = g;
      backgroundColor[2] = b;
      backgroundColor[3] = a;
      repaint();
   }
   
   public void setBackgroundColor (float[] rgba) {
      if (rgba.length < 3) {
         throw new IllegalArgumentException ("rgba must have length >= 3");
      }
      float alpha = rgba.length > 3 ? rgba[3] : 1f;
      setBackgroundColor (rgba[0], rgba[1], rgba[2], alpha);
   }

   public void setBackgroundColor (Color color) {
      color.getComponents (backgroundColor);
      repaint();
   }

   public Color getBackgroundColor() {
      return new Color (backgroundColor[0], backgroundColor[1], backgroundColor[2], backgroundColor[3]);
   }

   public float[] getBackgroundColor(float[] rgba) {
      if (rgba == null) {
         rgba = new float[4];
      }
      for (int i=0; i<rgba.length; ++i) {
         rgba[i] = backgroundColor[i];
      }
      return rgba;
   }

   public void setDefaultLights() {

      //      // For debugging lights, set to R-G-B
      //      float light0_ambient[] = { 0.2f, 0.2f, 0.2f, 1.0f };
      //      float light0_diffuse[] = { 0.8f, 0.0f, 0.0f, 1.0f };
      //      float light0_specular[] = { 0, 0, 0, 1 };
      //      float light0_position[] = { 1, 0, 0, 0 };
      //      
      //      float light1_ambient[] = { 0.0f, 0.0f, 0.0f, 1.0f };
      //      float light1_diffuse[] = { 0.0f, 0.8f, 0.0f, 1.0f };
      //      float light1_specular[] = { 0.0f, 0.0f, 0.0f, 1.0f };
      //      float light1_position[] = { 0, 1, 0, 0 };
      //
      //      float light2_ambient[] = { 0.0f, 0.0f, 0.0f, 1.0f };
      //      float light2_diffuse[] = { 0.0f, 0.0f, 0.8f, 1.0f };
      //      float light2_specular[] = { 0.0f, 0.0f, 0.0f, 1.0f };
      //      float light2_position[] = { 0, 0, 1, 0 };
      
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
      lightManager.addLight(new Light (
         light0_position, light0_ambient, light0_diffuse, light0_specular));
      lightManager.addLight (new Light (
         light1_position, light1_ambient, light1_diffuse, light1_specular));
      lightManager.addLight(new Light (
         light2_position, light2_ambient, light2_diffuse, light2_specular));
      lightManager.setMaxIntensity(1.0f);
      
      myProgramInfo.setNumLights (lightManager.numLights ());
   }
   
   public boolean setLightingEnabled (boolean enable) {
      boolean prev = myViewerState.lightingEnabled;
      if (enable != prev) {
         myViewerState.lightingEnabled = enable;
         if (enable) {
            myProgramInfo.setShading (Shading.NONE);
         } else {
            myProgramInfo.setShading (getShading());
         }
      }
      
      return prev;
   }

   public boolean isLightingEnabled() {
      return myViewerState.lightingEnabled && myViewerState.shading != Shading.NONE;
   }

   public boolean setVertexColoringEnabled (boolean enable) {
      boolean prev = myViewerState.vertexColorsEnabled;
      myViewerState.vertexColorsEnabled = enable;
      return prev;
   }

   public boolean isVertexColoringEnabled() {
      return myViewerState.vertexColorsEnabled;
   }
   
   public boolean hasVertexColoring(){
      return (myViewerState.vertexColorsEnabled && myViewerState.colorMixing != ColorMixing.NONE); 
   }

   protected boolean isHSVColorInterpolationEnabled() {
      return myViewerState.hsvInterpolationEnabled;
   }

   public ColorInterpolation getColorInterpolation() {
      if (myViewerState.hsvInterpolationEnabled) {
         return ColorInterpolation.HSV;
      }
      else {
         return ColorInterpolation.RGB;
      }
   }

   /**
    * Sets the color interpolation method to be used
    * @param interp new color interpolation
    * @return  the previous value so it can be reset
    */
   public ColorInterpolation setColorInterpolation (ColorInterpolation interp) {
      ColorInterpolation prev = getColorInterpolation();
      myViewerState.hsvInterpolationEnabled = (interp==ColorInterpolation.HSV);
      
      myProgramInfo.setColorInterpolation (interp);
      if (interp != DEFAULT_COLOR_INTERPOLATION) {
         myNonDefaultColorSettings |= COLOR_INTERPOLATION_BIT;
      }
      else {
         myNonDefaultColorSettings &= ~COLOR_INTERPOLATION_BIT;
      }
      return prev;
   }

   protected void setHSVCColorInterpolationEnabled(boolean set) {
      setColorInterpolation (ColorInterpolation.HSV);
   }

   public boolean setTextureMappingEnabled (boolean enable) {
      boolean prev = myViewerState.textureMappingEnabled; 
      myViewerState.textureMappingEnabled = enable;
      return prev;
   }

   public boolean isTextureMappingEnabled() {
      return myViewerState.textureMappingEnabled;
   }
   
   public abstract boolean hasVertexColorMixing (ColorMixing cmix);
   
   @Override
   public ColorMixing setVertexColorMixing (ColorMixing cmix) {
      ColorMixing prev = myViewerState.colorMixing;
      if (hasVertexColorMixing(cmix)) {
         myViewerState.colorMixing = cmix;
         myProgramInfo.setVertexColorMixing (cmix);
         if (cmix != DEFAULT_COLOR_MIXING) {
            myNonDefaultGeneralSettings |= COLOR_MIXING_BIT;
         }
         else {
            myNonDefaultGeneralSettings &= ~COLOR_MIXING_BIT;
         }
      }
      return prev;
   }
   
   /**
    * Force all viewer state variables to be written.  Some state variables
    * will not be committed, depending on whether we are in "select" mode
    * @param gl context
    * @param state state to commit
    */
   protected void commitFullViewerState(GL2GL3 gl, ViewerState state) {
      
      myCommittedViewerState = state.clone ();

      if (isSelecting ()) {
         // if selecting, disable colors, multisamples and blending         
         gl.glColorMask (true, true, true, true);
         gl.glDisable (GL.GL_BLEND);
         gl.glDisable (GL.GL_MULTISAMPLE);
         
         myCommittedViewerState.colorEnabled = true;
         myCommittedViewerState.transparencyEnabled = false;
         myCommittedViewerState.blendingEnabled = false;
         myCommittedViewerState.multiSampleEnabled = false;
         
      } else {
         
         // otherwise, track info
         if (state.colorEnabled) {
            gl.glColorMask (true, true, true, true);
         } else {
            gl.glColorMask (false, false, false, false);
         }
         
         if (state.blendingEnabled) {
            gl.glEnable (GL.GL_BLEND);
         } else {
            gl.glDisable (GL.GL_BLEND);
         }
         gl.glBlendFunc (state.blendSFactor.glValue (), state.blendDFactor.glValue ());
         
         if (state.multiSampleEnabled) {
            gl.glEnable (GL.GL_MULTISAMPLE);
         } else {
            gl.glDisable (GL.GL_MULTISAMPLE);
         }
      }
      
      if (state.depthEnabled) {
         gl.glEnable (GL.GL_DEPTH_TEST);
      } else {
         gl.glDisable (GL.GL_DEPTH_TEST);
      }
      
      gl.glDepthMask (state.depthWriteEnabled);
      
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
    
      gl.glPointSize (state.pointSize);
      gl.glLineWidth (state.lineWidth);
     
      // vertexColorsEnabled;   // set manually in draw methods
      // textureMappingEnabled;   
      // hsvInterpolationEnabled;  
      // colorMixing;           // not available
      // transparencyFaceCulling;  // set manually in draw methods
      
   }
   
   protected void maybeCommitViewerState(GL2GL3 gl, ViewerState state) {
      
      if (isSelecting ()) {
         // if selecting, disable coloring and blending
         if (myCommittedViewerState.colorEnabled == false) {
            gl.glColorMask (true, true, true, true);
            myCommittedViewerState.colorEnabled = true;
         }
         
         if (myCommittedViewerState.blendingEnabled == true) {
            gl.glDisable (GL.GL_BLEND);
            myCommittedViewerState.blendingEnabled = false;
         }
         myCommittedViewerState.transparencyEnabled = false;
         
         if (myCommittedViewerState.multiSampleEnabled == true) {
            gl.glDisable (GL.GL_MULTISAMPLE);
            myCommittedViewerState.multiSampleEnabled = false;
         }
         
      } else {
         
         // otherwise, track info
         if (myCommittedViewerState.colorEnabled != state.colorEnabled) {
            if (state.colorEnabled) {
               gl.glColorMask (true, true, true, true);
            } else {
               gl.glColorMask (false, false, false, false);
            }
            myCommittedViewerState.colorEnabled = state.colorEnabled;
         }
         
         if (myCommittedViewerState.blendingEnabled != state.blendingEnabled) {
            if (state.blendingEnabled) {
               gl.glEnable (GL.GL_BLEND);
            } else {
               gl.glDisable (GL.GL_BLEND);
            }
            myCommittedViewerState.blendingEnabled = state.blendingEnabled;
         }
         
         if (myCommittedViewerState.blendSFactor != state.blendSFactor ||
            myCommittedViewerState.blendDFactor != state.blendDFactor) {
            gl.glBlendFunc (state.blendSFactor.glValue (), state.blendDFactor.glValue ());
            myCommittedViewerState.blendSFactor = state.blendSFactor;
            myCommittedViewerState.blendDFactor = state.blendDFactor;
         }
         
         myCommittedViewerState.transparencyEnabled = state.transparencyEnabled;
         
         if (myCommittedViewerState.multiSampleEnabled != state.multiSampleEnabled) {
            if (state.multiSampleEnabled) {
               gl.glEnable (GL.GL_MULTISAMPLE);
            } else {
               gl.glDisable (GL.GL_MULTISAMPLE);
            }
            myCommittedViewerState.multiSampleEnabled = state.multiSampleEnabled;
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
      
      if (myCommittedViewerState.depthWriteEnabled != state.depthWriteEnabled) {
         gl.glDepthMask (state.depthWriteEnabled);
         myCommittedViewerState.depthWriteEnabled = state.depthWriteEnabled;
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
      
      if (state.pointSize != myCommittedViewerState.pointSize) {
         gl.glPointSize (state.pointSize);
         myCommittedViewerState.pointSize = state.pointSize;
      }
      
      if (state.lineWidth != myCommittedViewerState.lineWidth) {
         gl.glLineWidth (state.lineWidth);
         myCommittedViewerState.lineWidth = state.lineWidth;
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
   protected void maybeUpdateViewerState(GL2GL3 gl) {
      
      // maybe update shading
      if (myCommittedViewerState == null) {
         commitFullViewerState (gl, myViewerState);  
      } else {
         maybeCommitViewerState(gl, myViewerState);
      }
   }
   
   @Override
   public ColorMixing getVertexColorMixing () {
      return myViewerState.colorMixing;
   }
   
   public abstract boolean hasColorMapMixing (ColorMixing cmix);
   
   @Override
   public ColorMapProps setColorMap (ColorMapProps props) {
      ColorMapProps old = myColorMapProps;
      if (hasColorMapping()) {
         if (props != null) {
            myColorMapProps = props.clone();
         } else {
            myColorMapProps = null;
         }
         myMappingsSet = true;
      }
      return old;
   }
   
   @Override
   public ColorMapProps getColorMap () {
      return myColorMapProps;
   }   

   @Override
   public NormalMapProps setNormalMap (NormalMapProps props) {
      NormalMapProps old = myNormalMapProps;
      if (hasNormalMapping()){
         if (props != null) {
            myNormalMapProps = props.clone();
         } else {
            myNormalMapProps = null;
         }
         myMappingsSet = true;
      }
      return old;
   }
   
   @Override
   public NormalMapProps getNormalMap () {
      return myNormalMapProps;
   }   

   @Override
   public BumpMapProps setBumpMap (BumpMapProps props) {
      BumpMapProps old = myBumpMapProps;
      if (hasBumpMapping()) {
         if (props != null) {
            myBumpMapProps = props.clone();
         } else {
            myBumpMapProps = null;
         }
         myMappingsSet = true;
      }
      return old;
   }

   @Override
   public BumpMapProps getBumpMap () {
      return myBumpMapProps;
   }   

   protected boolean isGammaCorrectionEnabled() {
      return gammaCorrectionEnabled;
   }

   protected void setGammaCorrectionEnabled(boolean set) {
      gammaCorrectionRequested = true;
      repaint();
   }

   public boolean setDepthEnabled(boolean set) {
      boolean prev = myViewerState.depthEnabled;
      myViewerState.depthEnabled = set;
      return prev;
   }

   public boolean isDepthEnabled() {
      return myViewerState.depthEnabled;
   }

   public boolean setDepthWriteEnabled(boolean set) {
      boolean prev = myViewerState.depthWriteEnabled;
      myViewerState.depthWriteEnabled = set;
      return prev;
   }
   
   public boolean isDepthWriteEnabled() {
      return myViewerState.depthWriteEnabled;
   }
   
   protected boolean isColorEnabled() {
      return myViewerState.colorEnabled;
   }

   protected void setColorEnabled(boolean enable) {
      myViewerState.colorEnabled = enable;
   }

   public Shading setShading(Shading shading) {
      Shading prev = myViewerState.shading;
      myViewerState.shading = shading;
      
      if (isLightingEnabled ()) {
         myProgramInfo.setShading (shading);
      }
      if (shading != DEFAULT_SHADING) {
         myNonDefaultGeneralSettings |= SHADING_BIT;
      }
      else {
         myNonDefaultGeneralSettings &= ~SHADING_BIT;
      }
      return prev;
   }

   public Shading getShading() {
      return myViewerState.shading;
   }
   
   protected void pushViewerState() {
      viewerStateStack.push(myViewerState.clone());
   }
   
   public boolean setRoundedPoints(boolean enable) {
      boolean prev = myViewerState.roundedPoints;
      myViewerState.roundedPoints = enable;
      
      myProgramInfo.setRoundPointsEnabled (enable);
      
      return prev;
   }
   
   public boolean getRoundedPoints() {
      return myViewerState.roundedPoints;
   }

   protected void popViewerState() {
      setViewerState(viewerStateStack.pop());
   }

   protected void setViewerState(ViewerState state) {  
      setLightingEnabled(state.lightingEnabled);
      setDepthEnabled(state.depthEnabled);
      setDepthWriteEnabled(state.depthWriteEnabled);
      setColorEnabled(state.colorEnabled);
      setVertexColoringEnabled(state.vertexColorsEnabled);
      setTextureMappingEnabled(state.textureMappingEnabled);
      setFaceStyle(state.faceMode);
      setShading(state.shading);
      setHSVCColorInterpolationEnabled(state.hsvInterpolationEnabled);
      setVertexColorMixing (state.colorMixing);
      setRoundedPoints(state.roundedPoints);
      setBlendingEnabled(state.blendingEnabled);
      setBlendSourceFactor (state.blendSFactor);
      setBlendDestFactor (state.blendDFactor);
      setTransparencyEnabled (state.transparencyEnabled);
      setTransparencyFaceCulling (state.transparencyFaceCulling);
   }

   public boolean isTransparencyEnabled() {
      return myViewerState.transparencyEnabled;
   }

   public void setTransparencyEnabled (boolean enable) {
      myViewerState.transparencyEnabled = enable;
   }
   
   protected void enableTransparency () {
      pushViewerState ();
      if (!getTransparencyFaceCulling ()) {
         setDepthWriteEnabled (false);
         setFaceStyle (FaceStyle.FRONT_AND_BACK);
      }
      setBlendingEnabled (true);
      setTransparencyEnabled (true);
   }

   protected void disableTransparency () {
      popViewerState();  // should reset everything
   }
   
   public boolean isBlendingEnabled() {
      return myViewerState.blendingEnabled;
   }
   
   public void setBlendingEnabled(boolean set) {
      myViewerState.blendingEnabled = set;
   }
   
   public BlendFactor getBlendSourceFactor() {
      return myViewerState.blendSFactor;
   }

   public void setBlendSourceFactor(BlendFactor factor) {
      myViewerState.blendSFactor = factor;
   }

   public BlendFactor getBlendDestFactor() {
      return myViewerState.blendDFactor;
   }

   public void setBlendDestFactor(BlendFactor factor) {
      myViewerState.blendDFactor = factor;
   }

   /*
    * set "up" vector for viewing matrix
    */
   public void setUpVector (Vector3d upVector) {
      myViewState.myUp.set (upVector);
      setEyeToWorld (getEye(), myViewState.myCenter, myViewState.myUp);
   }

   public Vector3d getUpVector() {
      return myViewState.myUp;
   }

   /**
    * Returns the actual up vector. This can differ from the default value
    * returned by getUpVector() if the rotation mode is CONTINUOUS, in which
    * case the up vector is allowed to deviate from the default value.
    */
   private Vector3d getActualUpVector() {
      Vector3d up = new Vector3d();
      if (myRotationMode == RotationMode.CONTINUOUS) {
         viewMatrix.R.getRow(1, up);         
      }
      else {
         up.set (myViewState.myUp);
      }
      return up;
   }

   /**
    * {@inheritDoc}
    */
   public boolean hasSelection() {
      return true;
   }
   
   /**
    * {@inheritDoc}
    */
   public boolean isSelecting() {
      return selectEnabled;
   }

   @Override
   public boolean setHighlightStyle (HighlightStyle style) {
      switch (style) {
         case NONE:
         case COLOR: {
            myHighlightStyle = style;
            return true;
         }
         default:
            return false;
      }
   }
   
   public boolean hasHighlightStyle (HighlightStyle style) {
      switch (style) {
         case NONE:
         case COLOR: {
            return true;
         }
         default:
            return false;
      }
   }

   public HighlightStyle getHighlightStyle() {
      return myHighlightStyle;
   }

   public void setHighlightColor (Color color) {
      color.getRGBComponents (myHighlightColor);
      myHighlightColorModified = true;
      repaint();
   }

   @Override
   public void getHighlightColor (float[] rgba) {
      if (rgba.length < 3) {
         throw new IllegalArgumentException (
            "Argument rgba must have length of at least 3");
      }
      rgba[0] = myHighlightColor[0];
      rgba[1] = myHighlightColor[1];
      rgba[2] = myHighlightColor[2];
      if (rgba.length > 3) {
         rgba[3] = myHighlightColor[3];
      }
   }

   /**
    * Special color used for selection (primarily for color-based selection,
    * although since the renderbuffers are typically not drawn, can be
    * used for any selection method).
    */
   public void setSelectingColor (Color color) {
      color.getRGBComponents (mySelectingColor);
      mySelectingColorModified = true;
   }

   /**
    * The material color to use if the renderer is currently performing a
    * selection render. This is mainly used for color-based selection.
    *
    * @param r red
    * @param g green
    * @param b blue
    * @param a alpha
    */
    public void setSelectingColor(float r, float g, float b, float a) {
      mySelectingColor[0] = r;
      mySelectingColor[1] = g;
      mySelectingColor[2] = b;
      mySelectingColor[3] = a;
      mySelectingColorModified = true;
   }

   public void getSelectingColor (float[] rgba) {
      if (rgba.length < 3) {
         throw new IllegalArgumentException (
            "Argument rgba must have length of at least 3");
      }
      rgba[0] = mySelectingColor[0];
      rgba[1] = mySelectingColor[1];
      rgba[2] = mySelectingColor[2];
      if (rgba.length > 3) {
         rgba[3] = mySelectingColor[3];
      }
   }
   
   @Override
   public FaceStyle setFaceStyle(FaceStyle style) {
      FaceStyle prev = myViewerState.faceMode;
      myViewerState.faceMode = style;
      if (style != DEFAULT_FACE_STYLE) {
         myNonDefaultGeneralSettings |= FACE_STYLE_BIT;
      }
      else {
         myNonDefaultGeneralSettings &= ~FACE_STYLE_BIT;
      }
      return prev;
   }

   @Override
   public FaceStyle getFaceStyle () {
      return myViewerState.faceMode;
   }

   public void setSelectionEnabled (boolean selection) {
      selectionEnabled = selection;
   }

   public boolean isSelectionEnabled() {
      return selectionEnabled;
   }

   public void setSelectOnPress (boolean enable) {
      selectOnPressP = enable;
   }

   public boolean getSelectOnPress() {
      return selectOnPressP;
   }
   
   public void setEllipticSelection (boolean enable) {
      ellipticSelectionP = enable;
   }

   public boolean getEllipticSelection() {
      return ellipticSelectionP;
   } 
   
   public Point3d getCenter() {
      return new Point3d (myViewState.myCenter);
   }

   /**
    * Sets the center point for the viewer, and adjusts the eye coordinates so
    * that the eye's -z axis is directed at the center point. The vertical
    * direction is specified by the current up vector.
    * 
    * @param c
    * new center location, in world coordinates
    * @see #getUpVector
    */
   public void setCenter (Point3d c) {
      myViewState.myCenter.set (c);
      setEyeToWorld (getEye(), myViewState.myCenter, myViewState.myUp);
   }

   public Point3d getEye() {
      Point3d eye = new Point3d();
      eye.inverseTransform(viewMatrix.R, viewMatrix.p);
      eye.negate();
      return eye;
   }

   /**
    * Moves the eye coordinates to a specifed location in world coordinates, and
    * adjusts the orientation so that the eye's -z axis is directed at the
    * center point. The vertical direction is specified by the current up
    * vector.
    * 
    * @param eye
    * new eye location, in world coordinates
    * @see #getCenter
    * @see #getUpVector
    */
   public void setEye (Point3d eye) {
      // eye.set (e);
      setEyeToWorld (eye, myViewState.myCenter, myViewState.myUp);
   }

   /**
    * Returns a transform from world coordinates to center coordinates, with the
    * axes aligned to match the current eyeToWorld transform. Seen through the
    * viewer, this will appear centered on the view frame with z pointing toward
    * the view, y pointing up, and x pointing to the right.
    */
   public RigidTransform3d getCenterToWorld() {
      RigidTransform3d X = new RigidTransform3d();
      synchronized (viewMatrix) {
         X.R.transpose(viewMatrix.R);  
      }
      X.p.set (myViewState.myCenter);
      return X;
   }
   
   protected void setDragBox (Rectangle box) {
      myDragBox = box;
   }

   protected Rectangle getDragBox() {
      return myDragBox;
   }

   public int getCellDivisions() {
      return myGrid.getCellDivisions();
   }

   public double getCellSize() {
      return myGrid.getCellSize();
   }

   public GLGridPlane getGrid() {
      return myGrid;
   }

   public Light addLight (
      float[] position, float[] ambient, float[] diffuse, float[] specular) {
      Light light = new Light (position, ambient, diffuse, specular);
      lightManager.addLight (light);
      myProgramInfo.setNumLights (lightManager.numLights ());
      return light;
   }
   
   public int addLight (Light light) {
      int idx = lightManager.numLights();
      lightManager.addLight (light);
      myProgramInfo.setNumLights (idx+1);
      return idx;
   }

   public void removeLight (int i) {
      if (lightManager.removeLight(i)) {
         myProgramInfo.setNumLights (lightManager.numLights ());
      }
   }

   public boolean removeLight (Light light) {
      if (lightManager.removeLight(light)) {
         myProgramInfo.setNumLights (lightManager.numLights ());
         return true;
      }
      else {
         return false;
      }
   }

   public Light getLight (int i) {
      return lightManager.getLight (i);
   }
   
   public int getIndexOfLight (Light light) {
      return lightManager.indexOfLight (light);
   }

   public int numLights() {
      return lightManager.numLights();
   }

   /**
    * Setup for a screenshot during the next render cycle
    * @param w width of shot
    * @param h height of shot
    * @param samples number of samples to use for the
    *        multisample FBO (does antialiasing)
    * @param file file to which the screen shot should be written
    * @param format format string
    */
   public abstract void setupScreenShot (
      int w, int h, int samples, File file, String format);

   public abstract void awaitScreenShotCompletion();

   /**
    * Allows you to explicitly enable or disable resizing of viewer
    * (may want to disable while recording video or while selecting)
    * @return old value
    */
   public boolean setAutoResizeEnabled(boolean enabled) {
      boolean old = autoResizeEnabled;
      if (enabled & !old) {
         resetViewVolume = true;  // trigger reset
      }
      autoResizeEnabled = enabled;
      return old;
   }
   
   public boolean isAutoResizeEnabled() {
      return autoResizeEnabled;
   }
   
   /**
    * Allows you to explicitly enable or disable automatic viewport
    * adjustment based on screen width/height
    * (may want to disable while recording video or while selecting)
    * @return old value
    */
   public boolean setAutoViewportEnabled(boolean enabled) {
      boolean old = autoViewportEnabled;
      if (enabled & !old) {
         resetViewport = true;  // trigger reset
      }
      autoViewportEnabled = enabled;
      return old;
   }

   public boolean isAutoViewportEnabled() {
      return autoViewportEnabled;
   }

   public abstract void setupScreenShot (
      int w, int h, File file, String format);

   public abstract boolean grabPending();

   public void setRotationMode (RotationMode mode) {
      if (myRotationMode != mode) {
         myRotationMode = mode;
         if (mode == RotationMode.DEFAULT) {
            // reset eye transform so that up vector matches the default up vector
            setEyeToWorld (getEye(), myViewState.myCenter, getUpVector());
         }
      }
   }

   public RotationMode getRotationMode() {
      return myRotationMode;
   }

   public boolean setTransparencyFaceCulling(boolean enable) {
      boolean prev = myViewerState.transparencyFaceCulling;
      myViewerState.transparencyFaceCulling = enable;
      return prev;
   }

   public boolean getTransparencyFaceCulling() {
      return myViewerState.transparencyFaceCulling;
   }

   public Vector3d getEyeZDirection() {
      Vector3d zdir = new Vector3d();
      viewMatrix.R.getRow(2, zdir);
      return zdir;
   }

   public void setGlobalRenderFlags(int flags) {
      myRenderFlags.setDefault (flags);
   }

   public int getRenderFlags () {
      return myRenderFlags.get(); 
   }

   public boolean begin2DRendering() {
      return begin2DRendering (getScreenWidth(), getScreenHeight());
   }
   
   public boolean has2DRendering() {
      return true;
   }
   
   public void begin2DRendering (
      double left, double right, double bottom, double top) {

      // save depth, lighting, face culling information
      pushViewerState();
      pushProjectionMatrix();
      pushViewMatrix();
      pushModelMatrix();
      pushTextureMatrix ();
      
      setOrthogonal2d(-1, 1, -1, 1);
      setViewMatrix(RigidTransform3d.IDENTITY);
      setModelMatrix2d (left, right, bottom, top);
      
      setDepthEnabled (false);
      setLightingEnabled (false);
      setFaceStyle (FaceStyle.FRONT_AND_BACK);
      //setModelMatrix(RigidTransform3d.IDENTITY);
      
      getModelMatrix (myDefaultModelMatrix2d);

      rendering2d = true;
   }

   public void finish2DRendering() {

      popTextureMatrix ();
      popModelMatrix();
      popViewMatrix();
      popProjectionMatrix();
      popViewerState();

      rendering2d = false;
   }

   @Override
   public boolean begin2DRendering (double w, double h) {
      if (rendering2d) {
         throw new IllegalStateException ("Already in 2D rendering mode");
      }     
      if (has2DRendering()) {
         begin2DRendering(0, w, 0, h);
         return true;
      }
      else {
         return false;
      }
   }

   @Override
   public void end2DRendering() {
      if (!rendering2d) {
         throw new IllegalStateException ("Not in 2D rendering mode");
      }     
      if (has2DRendering()) {
         finish2DRendering();
      }
   }

   @Override
   public boolean is2DRendering() {
      return rendering2d;
   }

   public int numSelectionQueriesNeeded() {
      int num = myInternalRenderList.numSelectionQueriesNeeded();
      RenderList elist = myExternalRenderList;
      if (elist != null) {
         num += elist.numSelectionQueriesNeeded();
      }
      return num;
   }

   public void beginSelectionQuery (int idx) {
      if (selectEnabled) {
         mySelector.beginSelectionQuery (idx);
      }
   }

   public void endSelectionQuery () {
      if (selectEnabled) {
         mySelector.endSelectionQuery ();
      }
   }

   public void beginSubSelection (IsSelectable s, int idx) {
      if (selectEnabled) {
         mySelector.beginSelectionForObject (s, idx);
      }
   }

   public void endSubSelection () {
      if (selectEnabled) {
         mySelector.endSelectionForObject ();
      }
   }

   public void setSelectionFilter (ViewerSelectionFilter filter) {
      mySelectionFilter = filter;
   }

   public ViewerSelectionFilter getSelectionFilter () {
      return mySelectionFilter;
   }

   public boolean isSelectable (IsSelectable s) {
      if (s.isSelectable()) {
         if (s.numSelectionQueriesNeeded() < 0 && mySelectionFilter != null) {
            return mySelectionFilter.isSelectable(s);
         }
         return true;
      }
      else {
         return false;
      }
   }

   public GLMouseListener getMouseHandler() {
      return myMouseHandler;
   }

   public void setMouseHandler(GLMouseListener handler) {

      if (myMouseHandler != null) { 
         canvas.removeMouseListener(myMouseHandler);
         canvas.removeMouseWheelListener (myMouseHandler);
         canvas.removeMouseMotionListener (myMouseHandler);
      }

      myMouseHandler = handler;

      canvas.addMouseListener(myMouseHandler);
      canvas.addMouseWheelListener (myMouseHandler);
      canvas.addMouseMotionListener (myMouseHandler);

   }

   public abstract void cleanupScreenShots();

   protected void setRenderingProgramMode(RenderingMode mode) {
      myProgramInfo.setMode (mode);
   }
   
   protected void computeProjectionMatrix() {

      // from frustrum info
      double[] pvals = null;
      double w = myFrustum.right-myFrustum.left;
      double h = myFrustum.top-myFrustum.bottom;
      double d = myFrustum.far-myFrustum.near;
      
      // adjust offset to account for proper bin depth
      double zoffset = 0;
      if (myFrustum.depthBitOffset != 0) {
         // XXX should be 2, but doesn't seem to work well... 512 works better?
         zoffset = -2.0*myFrustum.depthBitOffset/(1 << (myFrustum.depthBits-1));
      }
      
      if (myFrustum.orthographic) {
         pvals = new double[]{
                              2/w, 0, 0, -(myFrustum.right+myFrustum.left)/w,
                              0, 2/h, 0, -(myFrustum.top+myFrustum.bottom)/h,
                              0,0,-2/d, -(myFrustum.far+myFrustum.near)/d+zoffset,
                              0, 0, 0, 1
         };
      } else {
         pvals = new double[] {
                               2*myFrustum.near/w, 0, (myFrustum.right+myFrustum.left)/w, 0,
                               0, 2*myFrustum.near/h, (myFrustum.top+myFrustum.bottom)/h, 0,
                               0, 0, -(myFrustum.far+myFrustum.near)/d-zoffset, -2*myFrustum.near*myFrustum.far/d,
                               0, 0, -1, 0
         };
      }


      if (pickMatrix != null) {
         Matrix4d p = new Matrix4d(pvals);
         projectionMatrix.mul(pickMatrix, p);
         //         System.out.println("Pick projection:");
         //         System.out.println(projectionMatrix);
      } else {
         projectionMatrix.set(pvals);
         //         System.out.println("Projection:");
         //         System.out.println(projectionMatrix);
      }
      invalidateProjectionMatrix();
   }
  
   /**
    * Alternative to gluPickMatrix, pre-multiplies by appropriate matrix to
    * reduce size
    */
   protected void setPickMatrix(float x, float y, float deltax, float deltay, int[] viewport) {
      // pre-multiply by pick
      if (deltax <= 0 || deltay <= 0) { 
         return;
      }
      // projectionMatrix.setIdentity();

      pickMatrix = new Matrix4d();

      // scale
      pickMatrix.set(0, 0, viewport[2]/deltax);
      pickMatrix.set(1, 1, viewport[3]/deltay);
      pickMatrix.set(2, 2, 1);

      // translate
      pickMatrix.set(0, 3, (viewport[2] - 2 * (x - viewport[0])) / deltax);
      pickMatrix.set(1, 3, (viewport[3] - 2 * (y - viewport[1])) / deltay);
      pickMatrix.set(2, 3, 0);
      pickMatrix.set(3,3, 1);
      
      // pre-multiply
      projectionMatrix.mul(pickMatrix, projectionMatrix);

      invalidateProjectionMatrix ();

   }

   // used internally for selection
   public void clearPickMatrix() {
      pickMatrix = null;
      computeProjectionMatrix (); // recompute projection
   }

   public void setModelMatrix(AffineTransform3dBase m) {
      synchronized(modelMatrix) {
         if (modelMatrix.getClass() == m.getClass()) {
            modelMatrix.set(m);
         } else {
            modelMatrix = m.copy();
         }
         modelNormalMatrix = computeInverseTranspose(m.getMatrix());
      }
      invalidateModelMatrix();
   }

   public void setViewMatrix(RigidTransform3d v) {
      synchronized(viewMatrix) {
         viewMatrix.set(v);
      }
      // XXX
      if (Math.abs (viewMatrix.getOffset ().norm ()) < 1e-5) {
         // System.err.println ("bang"); Thread.dumpStack();
      }
      invalidateViewMatrix();
   }

   public void getModelMatrix(AffineTransform3d m) {
      m.set(modelMatrix);
   }
   
   public AffineTransform3dBase getModelMatrix() {
      return modelMatrix.copy();
   }

//   protected void resetModelMatrix() {
//      synchronized (modelMatrix) {
//         modelMatrix = new RigidTransform3d(); // reset to identity
//         modelNormalMatrix = new Matrix3d(modelMatrix.getMatrix());   
//      }
//      invalidateModelMatrix();
//   }

   protected boolean isModelMatrixRigid() {
      return (modelMatrix instanceof RigidTransform3d);
   }

   public void translateModelMatrix(Vector3d t) {
      translateModelMatrix (t.x, t.y, t.z);
      invalidateModelMatrix();
   }

   public void translateModelMatrix(double tx, double ty, double tz) {
      RigidTransform3d TR = new RigidTransform3d (tx, ty, tz);
      mulModelMatrix (TR);
   }

   public void rotateModelMatrix(double zdeg, double ydeg, double xdeg) {
      RigidTransform3d TR = new RigidTransform3d (
         0, 0, 0, 
         Math.toRadians(zdeg), Math.toRadians(ydeg), Math.toRadians(xdeg)); 
      mulModelMatrix (TR);
   }

   public void scaleModelMatrix(double s) {
      synchronized(modelMatrix) {
         AffineTransform3d am = new AffineTransform3d(modelMatrix);
         am.applyScaling(s, s, s);
         modelMatrix = am; // normal matrix is unchanged
      }
      invalidateModelMatrix();
   }

   public void scaleModelMatrix(double sx, double sy, double sz) {
      synchronized(modelMatrix) {
         AffineTransform3d am = new AffineTransform3d(modelMatrix);
         am.applyScaling(sx, sy, sz);
         modelMatrix = am;
         if (sx == 0) {
            sx = Double.MAX_VALUE;
         } else {
            sx = 1.0/sx;
         }
         if (sy == 0) {
            sy = Double.MAX_VALUE;
         } else {
            sy = 1.0/sy;
         }
         if (sz == 0) {
            sz = Double.MAX_VALUE;
         } else {
            sz = 1.0/sz;
         }
         modelNormalMatrix.scaleColumn(0, sx);
         modelNormalMatrix.scaleColumn(1, sy);
         modelNormalMatrix.scaleColumn(2, sz);
      }
      invalidateModelMatrix();
   }

   public void mulModelMatrix (AffineTransform3dBase trans) {
      synchronized(modelMatrix) {
         if (trans instanceof RigidTransform3d) {
            RigidTransform3d rigid = (RigidTransform3d)trans;
            modelMatrix.mul(rigid);
            modelNormalMatrix.mul(rigid.R);  
         }
         else {
            AffineTransform3d aff = new AffineTransform3d(modelMatrix);
            aff.mul(trans);
            modelMatrix = aff;
            modelNormalMatrix = computeInverseTranspose(aff.getMatrix());
         }
      }
      invalidateModelMatrix();
   }

   private void setFromTransform(Matrix4d X, AffineTransform3dBase T) {
      X.setSubMatrix(0, 0, T.getMatrix());
      Vector3d b = T.getOffset();
      X.m03 = b.x;
      X.m13 = b.y;
      X.m23 = b.z;
      X.m30 = 0;
      X.m31 = 0;
      X.m32 = 0;
      X.m33 = 1;
   }

   public void getModelMatrix (Matrix4d X) {
      setFromTransform(X, modelMatrix);
   }

   public void getViewMatrix (Matrix4d X) {
      setFromTransform(X, viewMatrix);
   }

   public void getProjectionMatrix (Matrix4d X) {
      X.set(projectionMatrix);
   }
   
   public Matrix4d getProjectionMatrix() {
      return projectionMatrix.clone ();
   }
   
   @Override
   public void setTextureMatrix(AffineTransform2dBase T) {
      synchronized(textureMatrix) {
         if (textureMatrix.getClass() == T.getClass()) {
            textureMatrix.set(T);
         } else {
            textureMatrix = T.copy();
         }
      }
      invalidateTextureMatrix();
   }
   
   public AffineTransform2dBase getTextureMatrix() {
      return textureMatrix.copy ();
   }
   
   public void getTextureMatrix (AffineTransform2d X) {
      X.set (textureMatrix);
   }

   protected void invalidateModelMatrix() {
      modelMatrixValidP = false;
      myModelMatrixSet = true;
   }

   protected void invalidateProjectionMatrix() {
      projectionMatrixValidP = false;
   }

   protected void invalidateViewMatrix() {
      viewMatrixValidP = false;
   }
   
   protected void invalidateTextureMatrix() {
      textureMatrixValidP = false;
   }

   public void pushViewMatrix() {
      viewMatrixStack.push(viewMatrix.copy());
      viewStateStack.push(myViewState);
   }

   public boolean popViewMatrix() {
      if (viewMatrixStack.size() == 0) {
         return false;
      } 
      viewMatrix = viewMatrixStack.pop();
      myViewState = viewStateStack.pop();
      
      // XXX
      if (Math.abs (viewMatrix.getOffset ().norm ()) < 1e-5) {
         // System.err.println ("bang"); Thread.dumpStack();
      }
      
      invalidateViewMatrix();
      return true;
   }

   public void pushModelMatrix() {
      modelMatrixStack.push(modelMatrix.copy());
      modelNormalMatrixStack.push(modelNormalMatrix.clone());
      myModelMatrixSet = true;
   }

   protected Matrix3d computeInverseTranspose(Matrix3dBase M) {
      Matrix3d out = new Matrix3d(M);
      if (!(M instanceof RotationMatrix3d)) {
         boolean success = out.invert();
         if (!success) {
            SVDecomposition3d svd3 = new SVDecomposition3d(M);
            svd3.pseudoInverse(out);
         }
         out.transpose();
      }
      return out;
   }

   public boolean popModelMatrix() {
      if (modelMatrixStack.size() == 0) {
         return false;
      } 
      modelMatrix = modelMatrixStack.pop();
      modelNormalMatrix = modelNormalMatrixStack.pop();
      invalidateModelMatrix();
      myModelMatrixSet = true;
      return true;
   }

   public void pushProjectionMatrix() {
      projectionMatrixStack.push(projectionMatrix.clone());
      frustrumStack.push(myFrustum.clone());
   }

   public boolean popProjectionMatrix() {
      if (projectionMatrixStack.size() == 0) {
         return false;
      } 
      projectionMatrix = projectionMatrixStack.pop();
      myFrustum = frustrumStack.pop();
      invalidateProjectionMatrix();
      return true;
   }
   
   public void pushTextureMatrix() {
      textureMatrixStack.push (textureMatrix.copy ());
   }
   
   public boolean popTextureMatrix() {
      if (textureMatrixStack.size () == 0) {
         return false;
      }
      textureMatrix = textureMatrixStack.pop ();
      invalidateTextureMatrix ();
      return true;
   }

   //==========================================================================
   //  Clip Planes
   //==========================================================================

   protected int numUsedClipPlanes() {
      int c = 0;
      for (GLClipPlane clip : myClipPlanes) {
         if (clip.isClippingEnabled()) {
            c++;
            if (clip.isSlicingEnabled()) {
               c++;
            }
         }
      }
      return c;
   }
   
   public int numFreeClipPlanes() {
      int c = numUsedClipPlanes ();
      if (c >= maxClipPlanes) {
         return 0;
      }
      return maxClipPlanes-c;
   }

   public int getMaxGLClipPlanes() {
      return maxClipPlanes;
   }

   public GLClipPlane addClipPlane () {
      return addClipPlane (null, 0);
   }

   public GLClipPlane addClipPlane (RigidTransform3d X, double size) {
      GLClipPlane clipPlane = new GLClipPlane();
      if (size <= 0) {
         size = centerDistancePerPixel()*getScreenWidth()/2;
      }
      if (X == null) {
         X = getCenterToWorld();
      }
      clipPlane.setMinSize (size);
      clipPlane.setGridToWorld (X);
      clipPlane.setOffset (size / 50.0);
      clipPlane.setGridVisible (true);
      clipPlane.setMinCellPixels (8);
      clipPlane.setDragger (DraggerType.Transrotator);

      addClipPlane (clipPlane);
      return clipPlane;
   }

   public boolean addClipPlane (GLClipPlane clipPlane) {
      clipPlane.setViewer (this);
      myClipPlanes.add (clipPlane);
      
      if (isVisible()) {
         rerender();
      }
      return true;
   }

   public GLClipPlane getClipPlane (int idx) {
      return myClipPlanes.get (idx);
   }

   public int getNumClipPlanes() {
      return myClipPlanes.size();
   }

   public GLClipPlane[] getClipPlanes () {
      return myClipPlanes.toArray (new GLClipPlane[0]);
   }

   public boolean removeClipPlane (GLClipPlane clipPlane) {
      if (myClipPlanes.remove (clipPlane)) {
         clipPlane.setViewer (null);
         if (isVisible()) {
            rerender();
         }
         return true;
      }
      else {
         return false;
      }
   }

   public void clearClipPlanes() {
      for (GLClipPlane clip : myClipPlanes) {
         clip.setViewer(null);
      }
      myClipPlanes.clear();
      if (isVisible()) {
         rerender();
      }
      myProgramInfo.setNumClipPlanes (0);
   }

   private float[] toFloat (Vector3d vec) {
      return new float[] {(float)vec.x, (float)vec.y, (float)vec.z};
   }
   
   //==========================================================================
   //  Drawing
   //==========================================================================

   public void drawSphere (Vector3d pnt, double rad) {
      drawSphere (toFloat(pnt), rad);
   }

   @Override
   public void drawCube (Vector3d pnt, double w) {
      drawCube(toFloat(pnt), w);
   }
   
   @Override
   public void drawBox (Vector3d pnt, Vector3d widths) {
      drawBox (toFloat(pnt), widths.x, widths.y, widths.z);
   }
   
   public void drawCylinder (
      Vector3d pnt0, Vector3d pnt1, double rad, boolean capped) {
      drawCylinder (toFloat(pnt0), toFloat(pnt1), rad, capped);
   }
   
   public void drawSpindle (
      Vector3d pnt0, Vector3d pnt1, double rad) {
      drawSpindle (toFloat(pnt0), toFloat(pnt1), rad);
   }
   
   public void drawCone (
      Vector3d pnt0, Vector3d pnt1, double rad0, double rad1, boolean capped) {
      drawCone (toFloat(pnt0), toFloat(pnt1), rad0, rad1, capped);
   }

   public void drawArrow (
      Vector3d pnt0, Vector3d pnt1, double rad, boolean capped) {
      drawArrow (toFloat(pnt0), toFloat(pnt1), rad, capped);      
   }
   
   public void drawLine (
      RenderProps props, float[] pnt0, float[] pnt1, boolean highlight) {
      drawLine (props, pnt0, pnt1, /*color=*/null, /*capped=*/true, highlight);
   }

   public void drawLine (
      RenderProps props, float[] pnt0, float[] pnt1, 
      boolean capped, boolean highlight) {
      drawLine (props, pnt0, pnt1, /*color=*/null, capped, highlight);
   }

//   public void drawLine (
//      RenderProps props, float[] coords0, float[] coords1, boolean capped,
//      boolean selected) {
//      drawLine (props, coords0, coords1, capped, null, selected);
//   }

//   public abstract void drawCone (
//      float[] pnt0, float[] pnt1, double rad0, double rad1, boolean capped);

//   public void drawCone(
//      RenderProps props, float[] coords0, float[] coords1) {
//      drawCone(props, coords0, coords1, false);
//   }

   public void drawPoint (Vector3d pnt) {
      drawPoint (new float[] {(float)pnt.x, (float)pnt.y, (float)pnt.z});
   }

   public void drawPoint (double px, double py, double pz) {
      drawPoint (new float[] {(float)px, (float)py, (float)pz});
   }

//   public void drawPoint (float x, float y, float z) {
//      drawPoint (new float[] {x, y, z});
//   }
//
   public void drawLine (Vector3d pnt0, Vector3d pnt1) {
      drawLine (toFloat (pnt0), toFloat (pnt1));
   }

   public void drawLine (
      double px0, double py0, double pz0, double px1, double py1, double pz1) {
      drawLine (
         new float[] {(float)px0, (float)py0, (float)pz0}, 
         new float[] {(float)px1, (float)py1, (float)pz1}); 
   }

//   public void drawLine (
//      float x0, float y0, float z0, float x1, float y1, float z1) {
//      drawLine (new float[] {x0, y0, z0}, new float[] {x1, y1, z1});
//   }
//   
   public void drawTriangle (Vector3d pnt0, Vector3d pnt1, Vector3d pnt2) {
      drawTriangle (toFloat(pnt0), toFloat(pnt1), toFloat(pnt2));
   }

//   public abstract void drawLines(float[] vertices, int flags);

//   public void drawLines(float[] vertices) {
//      drawLines(vertices, 0);
//   }
//
   protected void computeNormal(float[] p0, float[] p1, float[] p2, float[] normal) {
      float[] u = new float[3];
      float[] v = new float[3];
      u[0] = p1[0]-p0[0]; u[1] = p1[1]-p0[1]; u[2] = p1[2]-p0[2];
      v[0] = p2[0]-p0[0]; v[1] = p2[1]-p0[1]; v[2] = p2[2]-p0[2];
      normal[0] = u[1]*v[2]-u[2]*v[1];
      normal[1] = u[2]*v[0]-u[0]*v[2];
      normal[2] = u[0]*v[1]-u[1]*v[0];
   }

   @Override
   public void drawAxes(
      RigidTransform3d X, double len, int width, boolean highlight) {
      drawAxes (X, new double[] {len, len, len}, width, highlight);
   }
   
   @Override
   public void drawSolidAxes (
      RigidTransform3d X, double[] lens, double rad, boolean highlight) {
      
      boolean savedHighlighting = setHighlighting(highlight);
      // deal with transform and len
      double lx = lens[0];
      double ly = lens[1];
      double lz = lens[2];

      if (X == null) {
         X = RigidTransform3d.IDENTITY;
      }
      pushModelMatrix();

      mulModelMatrix(X);
      
      if (lx != 0) {
         setColor (Color.RED);
         drawArrow (Point3d.ZERO, new Point3d (lx, 0, 0), rad, true);
      }
      if (ly != 0) {
         setColor (Color.GREEN);
         drawArrow (Point3d.ZERO, new Point3d (0, ly, 0), rad, true);
      }
      if (lz != 0) {
         setColor (Color.BLUE);
         drawArrow (Point3d.ZERO, new Point3d (0, 0, lz), rad, true);
      }
      
      // revert matrix transform
      popModelMatrix();

      setHighlighting(savedHighlighting);
   }
   
   @Override
   public void drawSolidAxes (
      RigidTransform3d X, double len, double rad, boolean highlight) {
      
      drawSolidAxes (X, new double[] {len, len, len}, rad, highlight);
   }
   
   public abstract GLTextRenderer getTextRenderer();
   
   public void setDefaultFont(Font font) {
      getTextRenderer().setFont (font);
   }
   
   public Font getDefaultFont() {
      return getTextRenderer().getFont();
   }
   
   @Override
   public boolean hasTextRendering() {
      return true;
   }

   public Rectangle2D getTextBounds(Font font, String str, double emSize) {
      return getTextRenderer().getTextBounds (font, str, (float)emSize);
   }
   
   @Override
   public double drawText (String str, float[] pos, double emSize) {
      return drawText (getDefaultFont(), str, pos, emSize);
   }
   
   @Override
   public double drawText (Font font, String str, Vector3d pos, double emSize) {
      return drawText (font, str, toFloat(pos), emSize);
   }

   @Override
   public double drawText (String str, Vector3d pos, double emSize) {
      return drawText(getDefaultFont (), str, toFloat (pos), emSize);
   }
   
   /**
    * MUST BE CALLED by whatever frame when it is going down, will notify any 
    * shared resources that they can be cleared.  It is best to add it as a
    * WindowListener
    */
   public void dispose() {
      canvas.destroy ();
   }

   //======================================================
   // COLORS and MATERIALS
   //======================================================

   /**
    * {@inheritDoc}
    */
   public boolean setHighlighting (boolean enable) {
      boolean prev = (myActiveColor == ActiveColor.HIGHLIGHT);
      if (myHighlightStyle == HighlightStyle.COLOR) {
         if (enable != prev) {
            // indicate that we may need to update color state
            if (enable) {
               myNonDefaultColorSettings |= HIGHLIGHTING_BIT;
               myActiveColor = ActiveColor.HIGHLIGHT;
            }
            else {
               myNonDefaultColorSettings &= ~HIGHLIGHTING_BIT;
               myActiveColor = ActiveColor.DEFAULT;
            }
         }
      }
      else if (myHighlightStyle == HighlightStyle.NONE) {
         // don't do anything ...
      }
      else {
         throw new UnsupportedOperationException (
            "Unsupported highlighting: " + myHighlightStyle);
      }
      return prev;
   }
   
   @Override
   public boolean getHighlighting() {
      // for now, only color highlighting is implemented
      return myActiveColor == ActiveColor.HIGHLIGHT;
   }

   @Override
   public void setFrontColor (float[] rgba) {
      myCurrentMaterial.setDiffuse (rgba);
      myCurrentMaterialModified = true;
   }

   
   /**
    * Gets a copy of the current diffuse "color"
    * @param rgba array to fill, or null to create new array
    * @return filled array
    */
   public float[] getColor(float[] rgba) {
      return getFrontColor(rgba);
   }
   
   @Override
   public float[] getFrontColor (float[] rgba) {
      if (rgba == null) {
         rgba = new float[4];
      }
      myCurrentMaterial.getDiffuse (rgba);
      return rgba;
   }

   @Override
   public void setBackColor (float[] rgba) {
      if (rgba == null) {
         if (myBackColor != null) {
            myBackColor = null;
            myCurrentMaterialModified = true;
         }
         myNonDefaultColorSettings &= ~BACK_COLOR_BIT;
      } else {
         if (myBackColor == null) {
            myBackColor = new float[4];
         }
         myBackColor[0] = rgba[0];
         myBackColor[1] = rgba[1];
         myBackColor[2] = rgba[2];
         myBackColor[3] = (rgba.length > 3 ? rgba[3] : 1.0f);
         myCurrentMaterialModified = true;
         myNonDefaultColorSettings |= BACK_COLOR_BIT;
      }
   }

   public float[] getBackColor (float[] rgba) {
      if (myBackColor == null) {
         return null;
      }
      if (rgba == null) {
         rgba = new float[4];
      }
      rgba[0] = myBackColor[0];
      rgba[1] = myBackColor[1];
      rgba[2] = myBackColor[2];
      if (rgba.length > 3) {
         rgba[3] = myBackColor[3];         
      }
      return rgba;
   }
   
   @Override
   public void setEmission(float[] rgb) {
      myCurrentMaterial.setEmission (rgb);
      if (rgb[0] != DEFAULT_MATERIAL_EMISSION[0] ||
          rgb[1] != DEFAULT_MATERIAL_EMISSION[1] ||
          rgb[2] != DEFAULT_MATERIAL_EMISSION[2]) {
         myNonDefaultColorSettings |= EMISSION_BIT;
      }
      else {
         myNonDefaultColorSettings &= ~EMISSION_BIT;
      }
      myCurrentMaterialModified = true;
   }

   @Override
   public float[] getEmission(float[] rgb) {
      if (rgb == null) {
         rgb = new float[3];
      }
      myCurrentMaterial.getEmission (rgb);
      return rgb;
   }

   @Override
   public void setSpecular(float[] rgb) {
      myCurrentMaterial.setSpecular (rgb);
      if (rgb[0] != DEFAULT_MATERIAL_SPECULAR[0] ||
          rgb[1] != DEFAULT_MATERIAL_SPECULAR[1] ||      
          rgb[2] != DEFAULT_MATERIAL_SPECULAR[2]) {
         myNonDefaultColorSettings |= SPECULAR_BIT;
      }
      else {
         myNonDefaultColorSettings &= ~SPECULAR_BIT;
      }
      myCurrentMaterialModified = true;
   }

   @Override
   public float[] getSpecular(float[] rgb) {
      if (rgb == null) {
         rgb = new float[3];
      }
      myCurrentMaterial.getSpecular (rgb);
      return rgb;
   }

   @Override
   public void setShininess(float s) {
      myCurrentMaterial.setShininess(s);
      if (s != DEFAULT_MATERIAL_SHININESS) {
         myNonDefaultColorSettings |= SHININESS_BIT;
      }
      else {
         myNonDefaultColorSettings &= ~SHININESS_BIT;
      }
      myCurrentMaterialModified = true;
   }

   @Override
   public float getShininess () {
      return myCurrentMaterial.getShininess();
   }

   @Override
   public void setFrontAlpha (float a) {
      myCurrentMaterial.setAlpha(a);
      myCurrentMaterialModified = true;
   }

   @Override
   public void setColor (float[] rgba, boolean highlight) {
      setFrontColor (rgba);
      setBackColor (null);
      setHighlighting (highlight);      
   }
   
//   public void setColorSelected() {
//      // do we need to set front color?
//      setFrontColor (mySelectedColor);
//      setBackColor (null);
//      setSelectionHighlighting (true);         
//   }

   @Override
   public void setColor (float[] rgba) {
      setFrontColor (rgba);
      setBackColor (null);
   }

   @Override
   public void setColor (float r, float g, float b) {
      setColor(new float[]{r,g,b,1.0f});
   }

   @Override
   public void setColor (float r, float g, float b, float a) {
      setColor(new float[]{r,g,b,a});
   }

   public void setColor (Color color) {
      setColor (color.getColorComponents(null));
   }
   
//   public void setColor (Color color) {
//      setColor(color.getColorComponents (new float[4]));
//   }

//   public void setColor (float[] frontRgba, float[] backRgba) {
//      setColor(frontRgba, backRgba, false);
//   }

//   public void setColor (Color frontColor, Color backColor) {
//      float[] frontRgba = frontColor.getColorComponents(new float[4]);
//      float[] backRgba = backColor.getColorComponents(new float[4]);
//      setColor(frontRgba, backRgba);
//   }

//   @Override
//   public void setColor (float[] frontRgba, float[] backRgba, boolean selected) {
//      setFrontColor (frontRgba);
//      setBackColor (backRgba);
//      setMaterialSelected (selected);
//   }

   public void setMaterial (
      float[] frontRgba, float[] backRgba, float shininess, boolean selected) {
      setFrontColor (frontRgba);
      setBackColor (backRgba);
      setShininess (shininess);
      setHighlighting (selected);
      setEmission (DEFAULT_MATERIAL_EMISSION);
      setSpecular (DEFAULT_MATERIAL_SPECULAR);
   }

//   @Override
//   public void setMaterial (float[] rgba) {
//      setMaterial(rgba, rgba, DEFAULT_MATERIAL_SHININESS, false);
//   }

   public void setPropsColoring (
      RenderProps props, float[] rgba, boolean highlight) {

      setHighlighting (highlight);         
      setFrontColor (rgba);
      if (rgba.length == 3) {
         setFrontAlpha ((float)props.getAlpha());
      }
      setBackColor (null);
      setShininess (props.getShininess());
      setEmission (DEFAULT_MATERIAL_EMISSION);
      float[] specular = props.getSpecularF();
      setSpecular (specular != null ? specular : DEFAULT_MATERIAL_SPECULAR);
   }
   
   public void setLineColoring (RenderProps props, boolean highlight) {
      setPropsColoring (props, props.getLineColorF(), highlight);
   }

   public void setPointColoring (RenderProps props, boolean highlight) {
      setPropsColoring (props, props.getPointColorF(), highlight);
   }

   public void setEdgeColoring (RenderProps props, boolean highlight) {
      float[] rgba = props.getEdgeColorF();
      if (rgba == null) {
         rgba = props.getLineColorF();
      }
      setPropsColoring (props, rgba, highlight);
   }

   public void setFaceColoring (RenderProps props, boolean highlight) {
      setFaceColoring (props, props.getFaceColorF(), highlight);
   }

   public void setFaceColoring (
      RenderProps props, float[] rgba, boolean highlight) {

      setFrontColor (rgba);
      if (rgba.length == 3) {
         setFrontAlpha ((float)props.getAlpha());
      }
      setBackColor (props.getBackColorF());
      setShininess (props.getShininess());
      setEmission (DEFAULT_MATERIAL_EMISSION);
      float[] specular = props.getSpecularF();
      setSpecular (specular != null ? specular : DEFAULT_MATERIAL_SPECULAR);
      setHighlighting (highlight);         
   }

   /**
    * {@inheritDoc}
    */
   public Shading setPointShading (RenderProps props) {
      Shading prevShading = getShading();
      Shading shading;
      if (props.getPointStyle() == PointStyle.POINT) {
         shading = Shading.NONE;
      }
      else {
         shading = props.getShading();
      }
      setShading (shading);
      return prevShading;
   }
   
   /**
    * {@inheritDoc}
    */
   public Shading setLineShading (RenderProps props) {
      Shading prevShading = getShading();
      Shading shading;
      if (props.getLineStyle() == LineStyle.LINE) {
         shading = Shading.NONE;
      }
      else {
         shading = props.getShading();
      }
      setShading (shading);      
      return prevShading;
   }
   
   /**
    * {@inheritDoc}
    */
   public Shading setPropsShading (RenderProps props) {
      Shading prevShading = getShading();
      setShading (props.getShading());      
      return prevShading;
   }

   //=======================================================
   // RENDEROBJECT
   //=======================================================
   
   @Override
   public void drawPoints(RenderObject robj) {
      drawPoints(robj, robj.getPointGroupIdx ());
   }
   
   @Override
   public void drawPoints (RenderObject robj, PointStyle style, double rad) {
      drawPoints(robj, robj.getPointGroupIdx (), style, rad);
   }
   
   @Override
   public void drawPoints(RenderObject robj, int gidx, PointStyle style, double rad) {
      drawPoints(robj, gidx, 0, robj.numPoints (gidx), style, rad);
   }
   
   @Override
   public void drawLines (RenderObject robj) {
      drawLines(robj, robj.getLineGroupIdx ());
   }
   
   @Override
   public void drawLines (RenderObject robj, LineStyle style, double rad) {
      drawLines(robj, robj.getLineGroupIdx (), style, rad);
   }
   
   @Override
   public void drawLines(RenderObject robj, int gidx, LineStyle style, double rad) {
      drawLines(robj, gidx, 0, robj.numLines (gidx), style, rad);
   }
   
   @Override
   public void drawTriangles(RenderObject robj) {
      drawTriangles(robj, robj.getTriangleGroupIdx ());
   }
   
   @Override
   public void drawTriangles(RenderObject robj, int gidx) {
      drawTriangles(robj, gidx, 0, robj.numTriangles (gidx));
   }
   
   @Override
   public void draw (RenderObject robj) {
      drawPoints (robj);
      drawLines (robj);
      drawTriangles (robj);
   }
   
   
   @Override
   public void drawVertices (
      RenderObject robj, VertexIndexArray idxs, DrawMode mode) {
      drawVertices(robj, idxs, 0, idxs.size (), mode);
   }

   //=======================================================
   // IMMEDIATE DRAW
   //=======================================================

   // data for "drawMode"
   protected DrawMode myDrawMode = null;
   protected boolean myDrawHasNormalData = false;
   protected boolean myDrawHasColorData = false;
   protected boolean myDrawHasTexcoordData = false;
   protected int myDrawVertexIdx = 0;
   protected float[] myDrawCurrentNormal = new float[3];
   protected float[] myDrawCurrentColor = new float[4];
   protected float[] myDrawCurrentTexcoord = new float[2];
   protected int myDrawDataCap = 0;
   protected float[] myDrawVertexData = null;
   protected float[] myDrawNormalData = null;
   protected float[] myDrawColorData = null;
   protected float[] myDrawTexcoordData = null;
   
   // Doing 2D rendering
   protected boolean rendering2d = false;
   protected AffineTransform3d myDefaultModelMatrix2d = new AffineTransform3d();

   protected void ensureDrawDataCapacity () {
      if (myDrawVertexIdx >= myDrawDataCap) {
         int cap = myDrawDataCap;
         if (cap == 0) {
            cap = 1000;
            myDrawVertexData = new float[3*cap];
            if (myDrawHasNormalData) {
               myDrawNormalData = new float[3*cap];
            }
            if (myDrawHasColorData) {
               myDrawColorData = new float[4*cap];
            }
            if (myDrawHasTexcoordData) {
               myDrawTexcoordData = new float[2*cap];
            }
         }
         else {
            cap = (int)(cap*1.5); // make sure cap is a multiple of 3
            myDrawVertexData = Arrays.copyOf (myDrawVertexData, 3*cap);
            if (myDrawHasNormalData) {
               myDrawNormalData = Arrays.copyOf (myDrawNormalData, 3*cap);
            }
            if (myDrawHasColorData) {
               myDrawColorData = Arrays.copyOf (myDrawColorData, 4*cap);
            }
            if (myDrawHasTexcoordData) {
               myDrawTexcoordData = Arrays.copyOf(myDrawTexcoordData, 2*cap);
            }
         }
         myDrawDataCap = cap;
      }
   }      

   /**
    * Returns either Selected (if selected color is currently active)
    * or the current material's diffuse color otherwise
    * 
    * @return current color being used by this viewer
    */
   private float[] getCurrentColor() {
      switch (myActiveColor) {
         case HIGHLIGHT:
            return myHighlightColor;
         case SELECTING:
            return mySelectingColor;
         case DEFAULT:
         default:
            return myCurrentMaterial.getDiffuse();
      }
   }

   @Override
   public void beginDraw (DrawMode mode) {
      if (myDrawMode != null) {
         throw new IllegalStateException (
         "Currently in draw mode (i.e., beginDraw() has already been called)");
      }
      myDrawMode = mode;
      myDrawVertexIdx = 0;
      myDrawHasNormalData = false;
      myDrawHasColorData = false;
      myDrawHasTexcoordData = false;

      myDrawCurrentColor = Arrays.copyOf (getCurrentColor(), 4);
      
      // update materials, because we are going to use 
      // myCurrentMaterialModified to trigger vertex-based coloring
      maybeUpdateMaterials();
   }

   @Override
   public void addVertex (float px, float py, float pz) {
      if (myDrawMode == null) {
         throw new IllegalStateException (
            "Not in draw mode (i.e., beginDraw() has not been called)");
      }
      ensureDrawDataCapacity();

      // check if we need colors
      if (!myDrawHasColorData && myCurrentMaterialModified) {
         // we need to store colors
         myDrawHasColorData = true;
         myDrawColorData = new float[4*myDrawDataCap];
         int cidx = 0;
         // back-fill colors
         for (int i=0; i<myDrawVertexIdx; ++i) {
            myDrawColorData[cidx++] = myDrawCurrentColor[0];
            myDrawColorData[cidx++] = myDrawCurrentColor[1];
            myDrawColorData[cidx++] = myDrawCurrentColor[2];
            myDrawColorData[cidx++] = myDrawCurrentColor[3];
         }
      }

      int vbase = 3*myDrawVertexIdx;
      if (myDrawHasNormalData) {
         myDrawNormalData[vbase  ] = myDrawCurrentNormal[0];
         myDrawNormalData[vbase+1] = myDrawCurrentNormal[1];
         myDrawNormalData[vbase+2] = myDrawCurrentNormal[2];
      }
      if (myDrawHasColorData) {
         int cbase = 4*myDrawVertexIdx;
         myDrawCurrentColor = getCurrentColor ();
         myDrawColorData[cbase  ] = myDrawCurrentColor[0];
         myDrawColorData[cbase+1] = myDrawCurrentColor[1];
         myDrawColorData[cbase+2] = myDrawCurrentColor[2];
         myDrawColorData[cbase+3] = myDrawCurrentColor[3];
      }
      if (myDrawHasTexcoordData) {
         int tbase = 2*myDrawVertexIdx;
         myDrawTexcoordData[tbase ] = myDrawCurrentTexcoord[0];
         myDrawTexcoordData[tbase+1] = myDrawCurrentTexcoord[1];
      }
      myDrawVertexData[vbase] = px;
      myDrawVertexData[++vbase] = py;
      myDrawVertexData[++vbase] = pz;
      ++myDrawVertexIdx;
   }

   @Override
   public void setNormal (float nx, float ny, float nz) {
      if (myDrawMode == null) {
         throw new IllegalStateException (
            "Not in draw mode (i.e., beginDraw() has not been called)");
      }
      myDrawCurrentNormal[0] = nx;
      myDrawCurrentNormal[1] = ny;
      myDrawCurrentNormal[2] = nz;
      if (!myDrawHasNormalData) {
         // back-fill previous normal data
         for (int i=0; i<myDrawVertexIdx; i++) {
            myDrawNormalData[i] = 0f;
         }
         myDrawHasNormalData = true;
      }
   }
   
   @Override
   public void setTextureCoord(float x, float y) {
      if (myDrawMode == null) {
         throw new IllegalStateException (
            "Not in draw mode (i.e., beginDraw() has not been called)");
      }
      myDrawCurrentTexcoord[0] = x;
      myDrawCurrentTexcoord[1] = y;
      if (!myDrawHasTexcoordData) {
         // back-fill previous normal data
         for (int i=0; i<myDrawVertexIdx; i++) {
            myDrawTexcoordData[i] = 0f;
         }
         myDrawHasTexcoordData = true;
      }
   }
   
   @Override
   public void addVertex (double px, double py, double pz) {
      addVertex ((float)px, (float)py, (float)pz);
   }

   @Override
   public void addVertex (Vector3d pnt) {
      addVertex ((float)pnt.x, (float)pnt.y, (float)pnt.z);
   }

   @Override
   public void addVertex (float[] pnt) {
      addVertex (pnt[0], pnt[1], pnt[2]);
   }

   @Override
   public void setNormal (double nx, double ny, double nz) {
      setNormal ((float)nx, (float)ny, (float)nz);
   }

   @Override
   public void setNormal (Vector3d nrm) {
      setNormal ((float)nrm.x, (float)nrm.y, (float)nrm.z);
   }

   @Override
   public void setTextureCoord (double tx, double ty) {
      setTextureCoord((float)tx, (float)ty);
   }
   
   @Override
   public void setTextureCoord (Vector2d tex) {
      setTextureCoord ((float)tex.x, (float)tex.y);
   }
   
   protected int getDrawPrimitive (DrawMode mode) {
      switch (mode) {
         case POINTS:
            return GL.GL_POINTS;
         case LINES:
            return GL.GL_LINES;
         case LINE_LOOP:
            return GL.GL_LINE_LOOP;
         case LINE_STRIP:
            return GL.GL_LINE_STRIP;
         case TRIANGLES:
            return GL.GL_TRIANGLES;
         case TRIANGLE_FAN:
            return GL.GL_TRIANGLE_FAN;
         case TRIANGLE_STRIP:
            return GL.GL_TRIANGLE_STRIP;
         default:
            throw new IllegalArgumentException (
               "Unknown VertexDrawMode: " + mode);
      }
   }

   protected abstract void doDraw(DrawMode drawMode,
      int numVertices, float[] vertexData, 
      boolean hasNormalData, float[] normalData, 
      boolean hasColorData, float[] colorData,
      boolean hasTexcoordData, float[] texcoordData);

   @Override
   public void endDraw() {
      if (myDrawMode == null) {
         throw new IllegalStateException (
            "Not in draw mode (i.e., beginDraw() has not been called)");
      }
      doDraw(myDrawMode, myDrawVertexIdx, myDrawVertexData, 
         myDrawHasNormalData, myDrawNormalData, 
         myDrawHasColorData, myDrawColorData,
         myDrawHasTexcoordData, myDrawTexcoordData);

      resetDraw();
   }
   
   private void resetDraw() {
      myDrawMode = null;

      myDrawDataCap = 0;
      myDrawVertexData = null;
      myDrawNormalData = null;
      myDrawColorData = null;
      myDrawTexcoordData = null;
      
      myDrawVertexIdx = 0;
      myDrawHasNormalData = false;
      myDrawHasColorData = false;
      myDrawHasTexcoordData = false;     
   }

   public abstract void maybeUpdateMaterials();
   
   public void restoreDefaultState(boolean strictChecking) {
      // check draw mode
      if (myMappingsSet) {
         if (myColorMapProps != null) {
            setColorMap (null);
         }
         if (myNormalMapProps != null) {
            setNormalMap (null);
         }
         if (myBumpMapProps != null) {
            setBumpMap (null);
         }
         myMappingsSet = false;
      }
      if (myNonDefaultColorSettings != 0) {
         if (myBackColor != null) {
            setBackColor (null);
         }
         if ((myNonDefaultColorSettings & EMISSION_BIT) != 0) {
            setEmission (DEFAULT_MATERIAL_EMISSION);
         }
         if ((myNonDefaultColorSettings & SPECULAR_BIT) != 0) {
            setSpecular (DEFAULT_MATERIAL_SPECULAR);
         }
         if ((myNonDefaultColorSettings & SHININESS_BIT) != 0) {
            setShininess (DEFAULT_MATERIAL_SHININESS);
         }
         if (getColorInterpolation() != DEFAULT_COLOR_INTERPOLATION) {
            setColorInterpolation (DEFAULT_COLOR_INTERPOLATION);
         }
         if (myActiveColor == ActiveColor.HIGHLIGHT) {
            setHighlighting (false);
         }
         myNonDefaultColorSettings = 0;
      }
      if (myNonDefaultGeneralSettings != 0) {
         if (myViewerState.faceMode != DEFAULT_FACE_STYLE) {
            setFaceStyle (DEFAULT_FACE_STYLE);
         }
         if ((myNonDefaultGeneralSettings & LINE_WIDTH_BIT) != 0) {
            setLineWidth (DEFAULT_LINE_WIDTH);
         }
         if ((myNonDefaultGeneralSettings & POINT_SIZE_BIT) != 0) {
            setPointSize (DEFAULT_POINT_SIZE);
         }
         if (myViewerState.shading != DEFAULT_SHADING) {
            setShading (DEFAULT_SHADING);
         }
//         if (mySurfaceResolution != DEFAULT_SURFACE_RESOLUTION) {
//            setSurfaceResolution (DEFAULT_SURFACE_RESOLUTION);
//         }
         if (myViewerState.colorMixing != DEFAULT_COLOR_MIXING) {
            setVertexColorMixing (DEFAULT_COLOR_MIXING);
         }
         if (getDepthOffset() != DEFAULT_DEPTH_OFFSET) {
            setDepthOffset (DEFAULT_DEPTH_OFFSET);
         }
         myNonDefaultGeneralSettings = 0;
      }
      if (myModelMatrixSet) {
         int mmsize = modelMatrixStack.size();
         if (rendering2d) {
            mmsize -= 1;
         }
         if (mmsize > 0) {
            if (strictChecking) {
               throw new IllegalStateException (
                  "render() method exited with model matrix stack size of " +
                   mmsize);
            }
            else {
               while (mmsize > 0) {
                  modelMatrixStack.pop();
                  mmsize--;
               }
            }
         }
         if (rendering2d) {
            synchronized (modelMatrix) {
               if (!modelMatrix.equals (myDefaultModelMatrix2d)) {
                  modelMatrix.set (myDefaultModelMatrix2d); // reset to default
                  invalidateModelMatrix();
               }
            }
         }
         else {
            synchronized (modelMatrix) {
               if (!modelMatrix.isIdentity()) {
                  modelMatrix = new RigidTransform3d(); // reset to identity
                  modelNormalMatrix = new Matrix3d();
                  invalidateModelMatrix();
               }
            }           
         }
         myModelMatrixSet = false;
      }
      if (myDrawMode != null) {
         if (strictChecking) {
            throw new IllegalStateException (
               "render() method exited while still in draw mode: "+myDrawMode);
         }
         else {
            resetDraw();
         }
      }
      
      // set front alpha if not one
      if (myCurrentMaterial.isTransparent()) {
         setFrontAlpha(1.0f);
      }
      
   }
   
   /**
    * Begin application-defined GL rendering, using GL primitives based
    * on handles returned by {@link #getGL()}, etc. Save the current
    * graphics state.
    */
   public GL beginGL() {
      pushViewerState();
      pushProjectionMatrix();
      pushModelMatrix();
      pushViewMatrix();
      pushTextureMatrix ();
      return getGL();
   }
   
   /**
    * Ends application-defined GL rendering. Restores the graphics state
    * to what it was when {@link #beginGL()} was called.
    */
   public void endGL() {
      // FINISH    
      popTextureMatrix ();
      popViewMatrix();
      popModelMatrix();
      popProjectionMatrix();
      myCommittedViewerState = null;    // clear committed info
      myCurrentMaterialModified = true; // force reset of materials
      popViewerState();
   }
   
}

