/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC), Antonio Sanchez (UBC),
 * and ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.GLUquadric;
import javax.swing.event.MouseInputListener;

import jogamp.opengl.glu.error.Error;
import maspack.matrix.AffineTransform3d;
import maspack.matrix.AxisAlignedRotation;
import maspack.matrix.AxisAngle;
import maspack.matrix.Matrix4d;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.Vector2d;
import maspack.matrix.Vector3d;
import maspack.properties.HasProperties;
import maspack.properties.Property;
import maspack.properties.PropertyList;
import maspack.render.RenderProps.LineStyle;
import maspack.render.RenderProps.Shading;
import maspack.util.InternalErrorException;

/**
 * @author John E Lloyd and ArtiSynth team members
 */
public class GLViewer implements GLEventListener, GLRenderer, HasProperties {

   //   public enum AxialView {
   //      POS_X_POS_Z, NEG_X_POS_Z, POS_X_POS_Y, 
   //      POS_X_NEG_Y, POS_Y_POS_Z, NEG_Y_POS_Z
   //   }

   protected static boolean myUseGLSelectSelection = false;

   private GL2 gl;
   private GLU glu;
   private GLCanvas canvas;
   private int width;
   private int height;
   Vector3d zDir = new Vector3d();     // used for determining zOrder
   private boolean rendering2d;

   /**
    * Class to set and hold rendering flags.  We enclose these in a class so
    * that we can access them in a synchronized way.
    */
   private class RenderFlags {
      int myDefaultFlags = 0;
      int mySpecialFlags = 0;
      boolean mySpecialSet = false;

      /**
       * Accessed by application to set default rendering flags.
       */
      void setDefault (int flags) {
         myDefaultFlags = flags;
      }

      /**
       * Accessed by application to request special flags for the next
       * render. Synchronized access is needed to set multiple values.
       */
      synchronized void setSpecial (int flags) {
         // 
         mySpecialFlags = flags;
         mySpecialSet = true;
      }

      /**
       * Accessed by rendering code to get the appropriate flags for the
       * current cycle. Synchronized access needed to check and get two values.
       */
      synchronized int get() {
         if (mySpecialSet) {
            mySpecialSet = false;
            return mySpecialFlags;
         }
         else {
            return myDefaultFlags;
         }
      }

      boolean isSpecialSet() {
         return mySpecialSet;
      }
   }

   RenderFlags myRenderFlags = new RenderFlags();

   // used to prevent view from re-scaling,
   // for instance while recording movie
   private boolean resizeEnabled = true;  
   private boolean myMultiSampleEnabled = false;

   private GLSelector mySelector;
   private GLSelectionFilter mySelectionFilter = null;

   private AxisAlignedRotation myAxialView = AxisAlignedRotation.X_Z;
   private AxisAlignedRotation myDefaultAxialView = AxisAlignedRotation.X_Z;

   /**
    * Enable selection via the (now deprecated) OpenGL select buffer
    * and <code>GL_SELECT</code> rendering mode mechanism.
    *
    * @param enable if true, enables select buffer selection
    */
   public static void enableGLSelectSelection (boolean enable) {
      myUseGLSelectSelection = enable;
   }

   public static boolean isGLSelectSelectionEnabled() {
      return myUseGLSelectSelection;
   }

   // More control over blending
   public static enum BlendType {
      GL_ONE_MINUS_CONSTANT_ALPHA(GL2.GL_ONE_MINUS_CONSTANT_ALPHA),
      GL_ONE_MINUS_SRC_ALPHA(GL2.GL_ONE_MINUS_SRC_ALPHA),
      GL_ONE(GL2.GL_ONE),
      GL_ZERO(GL2.GL_ZERO),
      GL_SRC_ALPHA(GL2.GL_SRC_ALPHA),
      ;

      private int myValue;
      private BlendType(int val) {
         myValue = val;
      }
      public int value() {
         return myValue;
      }
   }

   public enum RotationMode {
      DEFAULT, CONTINUOUS;
   }

   public static RotationMode DEFAULT_ROTATION_MODE = RotationMode.DEFAULT;
   public static BlendType DEFAULT_S_BLENDING = BlendType.GL_SRC_ALPHA;
   public static BlendType DEFAULT_D_BLENDING =
      BlendType.GL_ONE_MINUS_CONSTANT_ALPHA;
   public static boolean DEFAULT_ALPHA_FACE_CULLING = false;

   private RotationMode myRotationMode = DEFAULT_ROTATION_MODE;
   private BlendType sBlending = DEFAULT_S_BLENDING;
   private BlendType dBlending = DEFAULT_D_BLENDING;
   private boolean alphaFaceCulling = DEFAULT_ALPHA_FACE_CULLING;

   private RigidTransform3d Xtmp = new RigidTransform3d();
   private Vector3d utmp = new Vector3d();
   private Vector3d vtmp = new Vector3d();
   private float[] ctmp = new float[3];
   // buffers to store certain line styles
   double[] cosBuff = {1, 0, -1, 0, 1};
   double[] sinBuff = {0, 1, 0, -1, 0};
   private static double[] GLMatrix = new double[16];

   private LinkedList<GLRenderable> myRenderables =
      new LinkedList<GLRenderable>();
   private boolean myInternalRenderListValid = false;

   private RenderList myInternalRenderList = new RenderList();
   private RenderList myExternalRenderList;

   private boolean[] myGlPlaneAllocated = new boolean[6];
   private ArrayList<GLClipPlane> myClipPlanes = new ArrayList<GLClipPlane> (5);

   private double axisLength = 0;
   private boolean gridVisible = false;
   private boolean myTransparencyEnabledP = false;

   private float lmodel_ambient[] = { 0.0f, 0.0f, 0.0f, 0.0f };
   private float lmodel_twoside[] = { 0.0f, 0.0f, 0.0f, 0.0f };
   private float lmodel_local[] = { 0.0f, 0.0f, 0.0f, 0.0f };

   private float light0_ambient[] = { 0.1f, 0.1f, 0.1f, 1.0f };
   private float light0_diffuse[] = { 1.0f, 1.0f, 1.0f, 1.0f };
   private float light0_position[] = { -0.8660254f, 0.5f, 1f, 0f };
   private float light0_specular[] = { 0.5f, 0.5f, 0.5f, 1.0f };

   private float light1_ambient[] = { 0.0f, 0.0f, 0.0f, 1.0f };
   private float light1_diffuse[] = { 0.5f, 0.5f, 0.5f, 1.0f };
   private float light1_position[] = { 0.8660254f, 0.5f, 1f, 0f };
   private float light1_specular[] = { 0.5f, 0.5f, 0.5f, 1.0f };

   private float light2_ambient[] = { 0.0f, 0.0f, 0.0f, 1.0f };
   private float light2_diffuse[] = { 0.5f, 0.5f, 0.5f, 1.0f };
   private float light2_position[] = { 0f, -10f, 1f, 0f };
   private float light2_specular[] = { 0.5f, 0.5f, 0.5f, 1.0f };

   GLLightManager lightManager = null;

   protected LinkedList<Dragger3d> myDraggers;
   protected LinkedList<Dragger3d> myUserDraggers;
   protected MouseRayEvent myDraggerSelectionEvent;
   protected Dragger3d myDrawTool;
   protected GLAutoDrawable drawable;

   private LinkedList<MouseInputListener> myMouseInputListeners =
      new LinkedList<MouseInputListener>();
   private LinkedList<MouseWheelListener> myMouseWheelListeners =
      new LinkedList<MouseWheelListener>();

   protected GLMouseListener myMouseHandler;
   private boolean selectionEnabled = true;
   private boolean selectOnPressP = false;

   private double[] myClipPlaneValues = new double[4];

   public enum DraggerType {
      None, Rotator, Translator, Transrotator, Scalar
   };

   Point3d myCenter = new Point3d (0, 0, 0);
   Point3d myUp = new Point3d (0, 0, 1);

   RigidTransform3d XEyeToWorld = new RigidTransform3d();

   // frustum parameters
   private double myNear = 1;
   private double myFar = 1000;
   private double myLeft = -0.5;
   private double myRight = 0.5;
   private double myTop = 0.5;
   private double myBottom = -0.5;

   private double verticalFieldOfView = 30;
   private double fieldHeight = 10; // originally 10
   private boolean orthographicP = false;
   private boolean explicitFrustumP = false;

   private float[] bgColor = new float[] { 0f, 0f, 0f, 1f };
   private boolean bgColorRequest = true;
   private boolean resetViewVolume = false;

   private Rectangle myDragBox;

   double myWheelZoomScale = 10.0;

   private ArrayList<GLSelectionListener> mySelectionListeners =
      new ArrayList<GLSelectionListener>();

   private ArrayList<GLViewerListener> myViewerListeners =
      new ArrayList<GLViewerListener>();

   public static final double AUTO_FIT = -1.0;

   private int multipleSelectionMask = (InputEvent.CTRL_DOWN_MASK);

   private int dragSelectionMask = (InputEvent.SHIFT_DOWN_MASK);

   private int rotateButtonMask = (InputEvent.BUTTON2_DOWN_MASK);
   private int translateButtonMask =
      (InputEvent.BUTTON2_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
   private int zoomButtonMask =
      (InputEvent.BUTTON2_DOWN_MASK | InputEvent.CTRL_DOWN_MASK);

   private int selectionButtonMask = (InputEvent.BUTTON1_DOWN_MASK);

   private int selectionModMask = (InputEvent.SHIFT_DOWN_MASK);

   private GLGridPlane myGrid;

   public static PropertyList myProps = new PropertyList (GLViewer.class);

   private static final Point3d DEFAULT_VIEWER_CENTER = new Point3d();
   private static final Point3d DEFAULT_VIEWER_EYE = new Point3d (0, -1, 0);
   private static final AxisAlignedRotation DEFAULT_AXIAL_VIEW =
      AxisAlignedRotation.X_Z;

   static {
      myProps.add (
         "eye", "eye location (world coordinates)", DEFAULT_VIEWER_EYE);
      myProps.add (
         "center", "center location (world coordinates)",
         DEFAULT_VIEWER_CENTER);
      myProps.add (
         "axisLength", "length of rendered x-y-z axes", 0);
      myProps.add(
         "rotationMode", "method for interactive rotation",
         DEFAULT_ROTATION_MODE);
      myProps.add(
         "axialView", "axis-aligned view orientation",
         DEFAULT_AXIAL_VIEW);
      myProps.add(
         "defaultAxialView", "default axis-aligned view orientation",
         DEFAULT_AXIAL_VIEW);
      myProps.add (
         "backgroundColor", "background color", Color.BLACK);
      myProps.add(
         "sBlending", "source transparency blending", DEFAULT_S_BLENDING);
      myProps.add(
         "dBlending", "destination transparency blending", DEFAULT_D_BLENDING);
      myProps.add(
         "alphaFaceCulling", "allow transparency face culling",
         DEFAULT_ALPHA_FACE_CULLING);
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

   private void initLights() {
      if (drawable == null)
         return;

      gl = drawable.getGL().getGL2();
      for (GLLight light : lightManager.getLights()) {
         light.setupLight(gl);
      }

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

   public void setGridVisible (boolean visible) {
      gridVisible = visible;
   }

   public boolean getGridVisible() {
      return gridVisible;
   }

   public KeyListener[] getKeyListeners() {
      return getCanvas().getKeyListeners();
   }

   public void addKeyListener (KeyListener l) {
      getCanvas().addKeyListener(l);
   }

   public void removeKeyListener (KeyListener l) {
      getCanvas().removeKeyListener(l);
   }

   public LinkedList<Dragger3d> getDraggers() {
      // return all draggers except the internal clip plane dragger
      LinkedList<Dragger3d> list = new LinkedList<Dragger3d>();
      list.addAll (myUserDraggers);
      return list;
   }

   public void addSelectionListener (GLSelectionListener l) {
      mySelectionListeners.add (l);
   }

   public boolean removeSelectionListener (GLSelectionListener l) {
      return mySelectionListeners.remove (l);
   }

   public GLSelectionListener[] getSelectionListeners() {
      return mySelectionListeners.toArray (new GLSelectionListener[0]);
   }

   public void addViewerListener (GLViewerListener l) {
      myViewerListeners.add (l);
   }

   private void fireRerenderListeners() {
      if (myViewerListeners.size() > 0) {
         GLViewerEvent e = new GLViewerEvent (this);
         for (GLViewerListener l : myViewerListeners) {
            l.renderOccurred (e);
         }
      }
   }

   public boolean removeViewerListener (GLViewerListener l) {
      return myViewerListeners.remove (l);
   }

   public GLViewerListener[] getViewerListeners() {
      return myViewerListeners.toArray (new GLViewerListener[0]);
   }

   public synchronized void addRenderable (GLRenderable d) {
      myRenderables.add (d);
      myInternalRenderListValid = false;
   }

   public synchronized void addDragger (Dragger3d d) {
      myDraggers.add (d);
      myUserDraggers.add (d);
      myInternalRenderListValid = false;
      if (d instanceof Dragger3dBase) {
         ((Dragger3dBase)d).setViewer (this);
      }
   }

   public synchronized void setDrawTool (Dragger3d d) {
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

   public synchronized void removeRenderable (GLRenderable d) {
      myRenderables.remove (d);
      myInternalRenderListValid = false;
   }

   public synchronized void removeDragger (Dragger3d d) {
      if (d instanceof Dragger3dBase) {
         ((Dragger3dBase)d).setViewer (null);
      }
      myDraggers.remove (d);
      myUserDraggers.remove (d);
      myInternalRenderListValid = false;
   }

   public synchronized void clearDraggers() {
      for (Dragger3d d : myUserDraggers) {
         if (d instanceof Dragger3dBase) {
            ((Dragger3dBase)d).setViewer (null);
         }
         myDraggers.remove (d);
      }
      myUserDraggers.clear();
      myInternalRenderListValid = false;
   }

   public synchronized void clearRenderables() {
      myRenderables.clear();
      myInternalRenderListValid = false;
   }

   public double getNearClipPlaneZ() {
      return myNear;
   }

   public double getFarClipPlaneZ() {
      return myFar;
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
      return verticalFieldOfView;
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
      verticalFieldOfView = fov;
   }

   boolean selectEnabled = false;

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
   public void setPick (
      double x, double y, double w, double h,
      boolean ignoreDepthTest) {

      if (myUseGLSelectSelection) {
         mySelector = new GLSelectSelector (this);
      }
      else if (ignoreDepthTest) {
         mySelector = new GLOcclusionSelector(this);
      }
      else {
         mySelector = new GLColorSelector(this);
      }
      mySelector.setRectangle (x, y, w, h);
      selectEnabled = true;
      repaint();
   }

   public void repaint() {
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

   private void buildInternalRenderList() {
      myInternalRenderList.clear();
      myInternalRenderList.addIfVisibleAll (myRenderables);
      myInternalRenderListValid = true;
      // myInternalRenderList.addIfVisibleAll (myDraggers);
   }

   public synchronized void setExternalRenderList (RenderList list) {
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

   public boolean isVisible() {
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
      bounds[0] = myLeft;
      bounds[1] = myRight;
      bounds[2] = myBottom;
      bounds[3] = myTop;
      bounds[4] = myNear;
      bounds[5] = myFar;
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
      double left, double right, double bottom, double top, double near,
      double far) {
      this.myLeft = left;
      this.myRight = right;
      this.myBottom = bottom;
      this.myTop = top;
      this.myNear = near;
      this.myFar = far;
      this.explicitFrustumP = true;
      this.orthographicP = false;
      resetViewVolume = true;
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

      this.myTop = near * Math.tan (Math.toRadians (fieldOfView) / 2);
      this.myBottom = -this.myTop;
      this.myLeft = -aspect * myTop;
      this.myRight = -aspect * myBottom;
      this.myNear = near;
      this.myFar = far;

      this.verticalFieldOfView = fieldOfView;
      this.explicitFrustumP = false;
      this.orthographicP = false;
      resetViewVolume = true;
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

      this.myTop = fieldHeight / 2;
      this.myBottom = -this.myTop;
      this.myLeft = -aspect * myTop;
      this.myRight = -aspect * myBottom;
      this.myNear = near;
      this.myFar = far;
      this.fieldHeight = fieldHeight;
      this.orthographicP = true;
      resetViewVolume = true;
   }

   /**
    * Returns true if the current viewing projection is orthogonal.
    * 
    * @return true if viewing projection is orthogonal
    */
   public boolean isOrthogonal() {
      return orthographicP;
   }

   public void setOrthographicView (boolean enable) {
      if (enable) {
         autoFitOrtho (/* options= */0);
      }
      else {
         autoFitPerspective (/* options= */0);
      }
   }

   public boolean getBounds (Point3d pmin, Point3d pmax) {
      for (int i = 0; i < 3; i++) {
         pmin.set (i, Double.POSITIVE_INFINITY);
         pmax.set (i, Double.NEGATIVE_INFINITY);
      }
      boolean boundsSet = false;
      for (GLRenderable renderable : myRenderables) {
         renderable.updateBounds (pmin, pmax);
         boundsSet = true;
      }
      if (myExternalRenderList != null) {
         myExternalRenderList.updateBounds (pmin, pmax);
         boundsSet = true;
      }
      if (!boundsSet) {
         for (GLRenderable renderable : myDraggers) {
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

   /**
    * author: andrei autodetect the GLViewer size
    */

   public void autoFit() {
      if (isOrthogonal()) {
         autoFitOrtho (0);
      }
      else {
         autoFitPerspective (0); // check if size is affected via autofit
      }
   }

   private boolean hasRenderables() {
      return (
         myRenderables.size() > 0 ||
         (myExternalRenderList != null && myExternalRenderList.size() > 0) ||
         myDraggers.size() > 0 || 
         myDrawTool != null);
   }

   private void setGridSizeAndPosition (Point3d pcenter, double r) {

      myGrid.setMinSize (4 * r);
      myGrid.setPosition (pcenter);
      myGrid.setAutoSized (true);
      // redajust grid position so that it aligns with the resolution
      double res = myGrid.getResolution().getMajorCellSize();
      double x = res*Math.round(pcenter.x/res);
      double y = res*Math.round(pcenter.y/res);
      double z = res*Math.round(pcenter.z/res);
      myGrid.setPosition (new Point3d(x, y, z));
   }

   /**
    * Fits a perspective projection to the bounding box of the current set of
    * renderables, using the default vertical field of view ( as returned by
    * {@link #getVerticalFieldOfView getVerticalFieldOfView}). The eye
    * orientation is left unchanged, and the frustum is centered along the z
    * axis. The center point is set to the middle of the bounding box.
    */
   public synchronized void autoFitPerspective (int options) {
      if (hasRenderables()) {
         Point3d pcenter = new Point3d();
         double r = estimateRadiusAndCenter (pcenter);
         //if radius is zero, set default to radius 1
         if ( Math.abs(r) == 0 || Double.isInfinite(r) || Double.isNaN(r)) {
            r = 1;
         }
         double far = 40 * r;
         double near = far / 1000;

         myCenter.set (pcenter);
         Vector3d zdir = new Vector3d();
         XEyeToWorld.R.getColumn (2, zdir);
         double d = r / Math.sin (Math.toRadians (verticalFieldOfView) / 2);
         XEyeToWorld.p.scaledAdd (d, zdir, myCenter);

         setPerspective (verticalFieldOfView, near, far);
         setGridSizeAndPosition (pcenter, r);

         if (isVisible()) {
            rerender();
         }
      }
   }

   /**
    * Fits an orthogonal projection to the bounding box of the current set of
    * renderables. The eye orientation is left unchanged, and the frustum is
    * centered along the z axis. The center point is set to the middle of the
    * bounding box. The eye placed away from the center, along the -z axis.
    */

   /*
    * This was removed from the above documentation because autoFitView looks
    * like it has been removed.
    *  , at the same distance that would be set by
    * {@link #autoFitView autoFitView}
    */
   public synchronized void autoFitOrtho (int options) {
      if (hasRenderables()) {
         Point3d pcenter = new Point3d();
         double r = estimateRadiusAndCenter (pcenter);

         //if radius is zero, set default to radius 1
         if ( Math.abs(r) == 0  || Double.isInfinite(r) || Double.isNaN(r)) {
            r = 1;
         }

         myCenter.set (pcenter);
         Vector3d zdir = new Vector3d();
         XEyeToWorld.R.getColumn (2, zdir);
         double d = r / Math.sin (Math.toRadians (verticalFieldOfView) / 2);
         XEyeToWorld.p.scaledAdd (d, zdir, myCenter);

         double far = 40 * r;
         double near = far / 1000;
         setOrthogonal (2 * r, near, far);
         setGridSizeAndPosition (pcenter, r);

         if (isVisible()) {
            rerender();
         }
      }
   }

   /**
    * Sets the mouse button mask that enables rotation. This should be a
    * combination of the following mouse buttons and extended modifiers defined
    * in java.awt.event.InputEvent: BUTTON1_DOWN_MASK, BUTTON2_DOWN_MASK,
    * BUTTON2_DOWN_MASK, SHIFT_DOWN_MASK, ALT_DOWN_MASK, META_DOWN_MASK, and
    * CTRL_DOWN_MASK.
    * 
    * @param mask
    * rotation button mask
    */
   public void setRotateButtonMask (int mask) {
      rotateButtonMask = mask;
   }

   /**
    * Gets the mouse button mask that enables rotation.
    * 
    * @return rotation button mask
    * @see #setRotateButtonMask
    */
   public int getRotateButtonMask() {
      return rotateButtonMask;
   }

   /**
    * Sets the mouse button mask that enables translation. This should be a
    * combination of the following mouse buttons and extended modifiers defined
    * in java.awt.event.InputEvent: BUTTON1_DOWN_MASK, BUTTON2_DOWN_MASK,
    * BUTTON2_DOWN_MASK, SHIFT_DOWN_MASK, ALT_DOWN_MASK, META_DOWN_MASK, and
    * CTRL_DOWN_MASK.
    * 
    * @param mask
    * translation button mask
    */
   public void setTranslateButtonMask (int mask) {
      translateButtonMask = mask;
   }

   /**
    * Gets the mouse button mask that enables translation.
    * 
    * @return translation button mask
    * @see #setTranslateButtonMask
    */
   public int getTranslateButtonMask() {
      return translateButtonMask;
   }

   /**
    * Sets the mouse button mask that enables zooming. This should be a
    * combination of the following mouse buttons and extended modifiers defined
    * in java.awt.event.InputEvent: BUTTON1_DOWN_MASK, BUTTON2_DOWN_MASK,
    * BUTTON2_DOWN_MASK, SHIFT_DOWN_MASK, ALT_DOWN_MASK, META_DOWN_MASK, and
    * CTRL_DOWN_MASK.
    * 
    * @param mask
    * zooming button mask
    */
   public void setZoomButtonMask (int mask) {
      zoomButtonMask = mask;
   }

   /**
    * Gets the mouse button mask that enables zooming.
    * 
    * @return zooming button mask
    * @see #setZoomButtonMask
    */
   public int getZoomButtonMask() {
      return zoomButtonMask;
   }


   /**
    * set the mouse wheel zoom amount modified by Charles Krzysik on Apr 11th
    * 2008 default: 100
    * 
    * @param s
    * zoomAmount
    */
   public void setMouseWheelZoomScale (double s) {
      myWheelZoomScale = s; // originally s
   }

   /**
    * get the mouse wheel zoom amount default: 100
    * 
    * @return zoomAmount
    */
   public double getMouseWheelZoomScale() {
      return myWheelZoomScale;
   }

   /**
    * Sets the mouse button mask that enables selection. This should be a
    * combination of the following mouse buttons and extended modifiers defined
    * in java.awt.event.InputEvent: BUTTON1_DOWN_MASK, BUTTON2_DOWN_MASK,
    * BUTTON2_DOWN_MASK, SHIFT_DOWN_MASK, ALT_DOWN_MASK, META_DOWN_MASK, and
    * CTRL_DOWN_MASK.
    * 
    * @param mask
    * selection button mask
    */
   public void setSelectionButtonMask (int mask) {
      selectionButtonMask = mask;
   }

   /**
    * Gets the mouse button mask that enables selection.
    * 
    * @return selection button mask
    * @see #setSelectionButtonMask
    */
   public int getSelectionButtonMask() {
      return selectionButtonMask;
   }

   /**
    * Sets the modifier mask that enables multiple selection. This should be a
    * combination of the following extended modifiers defined in
    * java.awt.event.InputEvent: SHIFT_DOWN_MASK, ALT_DOWN_MASK, META_DOWN_MASK,
    * and CTRL_DOWN_MASK.
    * 
    * @param mask
    * multiple selection modifier mask
    */
   public void setMultipleSelectionMask (int mask) {
      multipleSelectionMask = mask;
   }

   /**
    * Gets the modifier mask that enables multiple selection.
    * 
    * @return multiple selection modifier mask
    * @see #setMultipleSelectionMask
    */
   public int getMultipleSelectionMask() {
      return multipleSelectionMask;
   }

   /**
    * Sets the modifier mask to enable drag selection. This mask should be a
    * combination of the following extended modifiers defined in
    * java.awt.event.InputEvent: SHIFT_DOWN_MASK, ALT_DOWN_MASK, META_DOWN_MASK,
    * and CTRL_DOWN_MASK.
    * 
    * @param mask
    * selection modifier mask
    */
   public void setDragSelectionMask (int mask) {
      dragSelectionMask = mask;
   }

   /**
    * Gets the modifier mask that enables drag selection.
    * 
    * @return drag selection modifier mask
    * @see #setDragSelectionMask
    */
   public int getDragSelectionMask() {
      return dragSelectionMask;
   }

   /**
    * Returns the mouse button modifiers that may accompany selection.
    * 
    * @return selection modifier mask
    */
   public int getSelectionModifierMask() {
      return selectionModMask;
   }

   public int getWidth() {
      return width;
   }

   public int getHeight() {
      return height;
   }

   public GL getGL() {
      return drawable.getGL();
   }

   public GLU getGLU() {
      return glu;
   }

   public void setAutoSwapBufferMode (boolean enable) {
      drawable.setAutoSwapBufferMode (enable);
   }

   public boolean getAutoSwapBufferMode() {
      return drawable.getAutoSwapBufferMode();
   }

   public GLContext getContext() {
      return drawable.getContext();
   }

   public void swapBuffers() {
      drawable.swapBuffers();
   }

   double centerDistance (int x, int y) {
      int dx = x - width / 2;
      int dy = y - height / 2;
      return Math.sqrt (dx * dx + dy * dy);
   }

   GLSelectionEvent selectionEvent;

   /**
    * Rotate the eye coordinate frame about the center point, independent
    * of the default up vector.
    * 
    * @param xang
    * amount of horizontal rotation (in radians)
    * @param yang
    * amount of vertical rotation (in radians)
    */
   synchronized void rotateContinuous (double xang, double yang) {

      Vector3d reye = new Vector3d();
      reye.sub (XEyeToWorld.p, myCenter);

      Vector3d yCam = new Vector3d();     // up-facing vector
      XEyeToWorld.R.getColumn(1, yCam);
      Vector3d xCam = new Vector3d();     // right-facing vector
      XEyeToWorld.R.getColumn(0, xCam);

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
      eye.add (reye, myCenter);
      setEyeToWorld (eye, myCenter, yCam);

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
   synchronized void rotateFixedUp (double xang, double yang) {
      Vector3d reye = new Vector3d();
      reye.sub (XEyeToWorld.p, myCenter);

      if (yang != 0) {
         Vector3d xCam = new Vector3d(); // right-facing vector
         XEyeToWorld.R.getColumn (0, xCam);

         double oldAngle = Math.acos (reye.dot (myUp) / reye.norm());
         if (!((yang < 0 && (-yang) > oldAngle) ||
            (yang > 0 && yang > (Math.PI - oldAngle)))) {
            reye.transform (new RotationMatrix3d (new AxisAngle (xCam, yang)));
         } 

      }
      if (xang != 0) {
         reye.transform (new RotationMatrix3d (new AxisAngle (myUp, xang)));
      }

      Point3d eye = new Point3d();
      eye.add (reye, myCenter);

      setEyeToWorld (eye, myCenter, myUp);

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
   synchronized void rotate (double xang, double yang) {

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
   synchronized void translate (double delx, double dely) {
      Vector3d xCam = new Vector3d(), yCam = new Vector3d();
      XEyeToWorld.R.getColumn (0, xCam);
      XEyeToWorld.R.getColumn (1, yCam);

      Vector3d offset = new Vector3d();
      offset.scale (-delx, xCam);
      offset.scaledAdd (-dely, yCam, offset);

      XEyeToWorld.p.add (offset);
      myCenter.add (offset);

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
   synchronized public void zoom (double s) {
      if (orthographicP) {
         fieldHeight *= s;
         resetViewVolume = true;
      }
      else {
         Vector3d reye = new Vector3d();
         reye.sub (XEyeToWorld.p, myCenter);
         reye.inverseTransform (XEyeToWorld);
         reye.x = reye.y = 0;
         reye.transform (XEyeToWorld);
         XEyeToWorld.p.scaledAdd (s - 1, reye);
      }
      repaint();
   }

   /**
    * Computes the distance per pixel for a point specified with respect to
    * world coordinates.
    */
   public double distancePerPixel (Vector3d pnt) {
      if (orthographicP) {
         return fieldHeight / height;
      }
      else {
         Point3d pntInEye = new Point3d (pnt);
         pntInEye.inverseTransform (XEyeToWorld);
         return Math.abs (pntInEye.z / myNear) * (myTop - myBottom) / height;
      }
   }

   /**
    * Computes the distance per pixel at the viewpoint center.
    */
   public double centerDistancePerPixel() {
      return distancePerPixel (myCenter);
   }

   private static Color darkRed = new Color (0.5f, 0, 0);
   private static Color darkGreen = new Color (0, 0.5f, 0);
   private static Color darkBlue = new Color (0, 0, 0.5f);


   private Color getAxisColor (int idx) {
      switch (idx) {
         case 0: {
            return darkRed;
         }
         case 1: {
            return darkGreen;
         }
         case 2: {
            return darkBlue;
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
      double d = XEyeToWorld.p.distance (myCenter);
      X.p.scaledAdd (d, zdir, myCenter);
      myUp.set (ydir);
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
      myGrid.setXAxisColor (getAxisColor (xmaxIdx));
      myGrid.setYAxisColor (getAxisColor (ymaxIdx));
   }

   // public static RotationMatrix3d getAxialViewOrientation (AxisAlignedRotation v) {
   //    switch (v) {
   //       case X_Z: {
   //          return new RotationMatrix3d(1, 0, 0,  0, 0, -1,  0, 1, 0);
   //       }
   //       case Y_Z: {
   //          return new RotationMatrix3d(0, 0, 1,  1, 0, 0,  0, 1, 0);
   //       }
   //       case NY_Z: {
   //          return new RotationMatrix3d(0, 0, -1,  -1, 0, 0,  0, 1, 0);
   //       }
   //       case NX_Z: {
   //          return new RotationMatrix3d(-1, 0, 0,  0, 0, 1,  0, 1, 0);
   //       }
   //       case X_Y: {
   //          return new RotationMatrix3d();
   //       }
   //       case X_NY: {
   //          return new RotationMatrix3d(1, 0, 0,  0, -1, 0,  0, 0, -1);
   //       }
   //       default: {
   //          throw new InternalErrorException ("Unimplemented view " + v);
   //       }
   //    }
   // }

   public void setDefaultAxialView (AxisAlignedRotation view) {
      setAxialView (view);
      myDefaultAxialView = view;
   }

   public AxisAlignedRotation getDefaultAxialView () {
      return myDefaultAxialView;
   }

   public void setAxialView (AxisAlignedRotation view) {
      setAlignedEyeOrientation (view.getMatrix());
      myAxialView = view;
   }

   public AxisAlignedRotation getAxialView() {
      return myAxialView;
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
   public GLViewer (int width, int height) {
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
   public GLViewer (GLViewer shareWith, int width, int height) {
      this (null, shareWith.getCanvas(), width, height);
   }

   /**
    * Creates a new GLViewer with specified capabilities and size.
    * 
    * @param cap
    * Desired GL capabilities. Can be specified as null, which will create
    * default capabilities.
    * @param shareWith
    * a GL drawable with which the GLCanvas is to share resources (e.g., display
    * lists and textures). Can be specified as null.
    * @param width
    * initial width of the viewer
    * @param height
    * initial height of the viewer
    */
   public GLViewer (GLCapabilities cap, GLAutoDrawable shareWith, int width,
      int height) {
      if (cap == null) {
         GLProfile glp2 = GLProfile.get(GLProfile.GL2);
         cap = new GLCapabilities(glp2);
         cap.setSampleBuffers (true);
         cap.setNumSamples (8);
      }

      GLContext sharedContext = null;
      if (shareWith != null) {
         sharedContext = shareWith.getContext();
      }


      // canvas = new GLCanvas(cap, sharedContext); //GLCanvas (cap, null, sharedContext, null);
      canvas = new GLCanvas(cap, null, null);
      if (sharedContext != null) {
         canvas.setSharedContext(sharedContext);
      }

      canvas.addGLEventListener (this);
      canvas.setSize (width, height);

      this.width = width;
      this.height = height;
      myDraggers = new LinkedList<Dragger3d>();
      myUserDraggers = new LinkedList<Dragger3d>();

      lightManager = new GLLightManager();
      setDefaultLights();

      XEyeToWorld = new RigidTransform3d (0, -1, 0, 1, 0, 0, Math.PI / 2);

      glu = new GLU();
      myGrid = new GLGridPlane();
      myGrid.setViewer (this);

      setAxialView (myAxialView);
   }

   public GLCanvas getCanvas() {
      return canvas;
   }

   public void setDefaultLights() {

      lightManager.clearLights();
      lightManager.addLight(new GLLight (
         light0_position, light0_ambient, light0_diffuse, light0_specular));
      lightManager.addLight (new GLLight (
         light1_position, light1_ambient, light1_diffuse, light1_specular));
      lightManager.addLight(new GLLight (
         light2_position, light2_ambient, light2_diffuse, light2_specular));

   }

   public void init (GLAutoDrawable drawable) {
      this.drawable = drawable;
      gl = drawable.getGL().getGL2();

      gl.setSwapInterval (1);

      if (gl.isExtensionAvailable("GL_ARB_multisample")) {
         gl.glEnable(GL2.GL_MULTISAMPLE);
         System.out.println ("multisample enabled = " +
            gl.glIsEnabled(GL2.GL_MULTISAMPLE));
         myMultiSampleEnabled = true;
      }

      gl.glEnable (GL2.GL_CULL_FACE);
      gl.glCullFace (GL2.GL_BACK);
      gl.glEnable (GL2.GL_DEPTH_TEST);
      gl.glClearDepth (1.0);

      gl.glLightModelfv (GL2.GL_LIGHT_MODEL_LOCAL_VIEWER, lmodel_local, 0);
      gl.glLightModelfv (GL2.GL_LIGHT_MODEL_TWO_SIDE, lmodel_twoside, 0);
      gl.glLightModelfv (GL2.GL_LIGHT_MODEL_AMBIENT, lmodel_ambient, 0);
      gl.glEnable (GL2.GL_LIGHTING);

      lightManager.init(gl);
      initLights();

      gl.glShadeModel (GL2.GL_FLAT);

      // gl.glFrontFace (GL2.GL_CW);

      if (bgColorRequest) {
         gl.glClearColor (bgColor[0], bgColor[1], bgColor[2], 1f);
         bgColorRequest = false;
      }

      resetViewVolume();

      myMouseHandler = new GLMouseListener (this);

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
      width = drawable.getWidth();
      height = drawable.getHeight();

      // Sanchez Sept 25, 2013:
      // On dual-monitor setups under Linux, OpenGL *sometimes* 
      // re-initializes when switching between screens.  This 
      // clears displays lists on hardware, causing a JOGL crash
      // when we try to access the old ones.  Thus, we need to 
      // remove all references manually
      // XXX This is a hack.  I am hoping I catch all display
      //     lists by looping through RenderProps objects.
      //     A more robust thing to do would be to do it in
      //     the render code, first check that a list
      //     actually exists before calling it
      buildInternalRenderList();
      for (int i=0; i<myInternalRenderList.size(); i++) {
         GLRenderable glr = myInternalRenderList.get(i);
         if (glr instanceof HasRenderProps) {
            HasRenderProps hrp = (HasRenderProps)glr;
            RenderProps rp = hrp.getRenderProps();
            if (rp != null) {
               rp.clearAllDisplayLists();
            }
         }
      }

      if (myExternalRenderList != null) {
         for (int i=0; i<myExternalRenderList.size(); i++) {
            GLRenderable glr = myExternalRenderList.get(i);
            if (glr instanceof HasRenderProps) {
               HasRenderProps hrp = (HasRenderProps)glr;
               RenderProps rp = hrp.getRenderProps();
               if (rp != null) {
                  rp.clearAllDisplayLists();
               }
            }
         }
      }

      // initDone = true;

   }

   public synchronized void addMouseInputListener (MouseInputListener l) {
      if (canvas != null) {
         canvas.addMouseListener (l);
         canvas.addMouseMotionListener (l);
      }
      myMouseInputListeners.add (l);
   }

   public synchronized void removeMouseInputListener (MouseInputListener l) {
      if (canvas != null) {
         canvas.removeMouseListener (l);
         canvas.removeMouseMotionListener (l);
      }
      myMouseInputListeners.remove (l);
   }

   public synchronized MouseInputListener[] getMouseInputListeners() {
      return myMouseInputListeners.toArray (new MouseInputListener[0]);
   }

   public synchronized void addMouseWheelListener (MouseWheelListener l) {
      if (canvas != null) {
         canvas.addMouseWheelListener (l);
      }
      myMouseWheelListeners.add (l);
   }

   public synchronized void removeMouseWheelListener (MouseWheelListener l) {
      if (canvas != null) {
         canvas.removeMouseWheelListener (l);
      }
      myMouseWheelListeners.remove (l);
   }

   public synchronized MouseWheelListener[] getMouseWheelListeners() {
      return myMouseWheelListeners.toArray (new MouseWheelListener[0]);
   }

   public void reshape (GLAutoDrawable drawable, int x, int y, int w, int h) {
      width = w;
      height = h;

      // only resize view volume if not recording
      if (resizeEnabled) {
         resetViewVolume();
      }

   }

   public double getViewPlaneHeight() {
      if (orthographicP) {
         return fieldHeight;
      }
      else {
         return myTop - myBottom;
      }
   }

   public double getViewPlaneWidth() {
      return (width / (double)height) * getViewPlaneHeight();
   }

   public double getViewPlaneDistance() {
      return myNear;
   }

   public void resetViewVolume(int width, int height) {

      gl.glMatrixMode (GL2.GL_PROJECTION);
      gl.glLoadIdentity();

      double aspect = width / (double)height;
      if (orthographicP) {
         double hh = fieldHeight / 2;
         gl.glOrtho (-aspect * hh, aspect * hh, -hh, hh, myNear, myFar);
      }
      else {
         if (explicitFrustumP) {
            gl.glFrustum (myLeft, myRight, myBottom, myTop, myNear, myFar);
         }
         else {
            myLeft = -aspect * myTop;
            myRight = -myLeft;
            gl.glFrustum (myLeft, myRight, myBottom, myTop, myNear, myFar);
            // glu.gluPerspective(verticalFieldOfView, width/(float)height,
            // myNear, myFar);
         }
      }

      gl.glMatrixMode (GL2.GL_MODELVIEW);
      gl.glViewport (0, 0, width, height);
   }

   // Sanchez, July 2013:
   // used to adjust selection volume, or else the orthogonal
   // view scale sometimes too large to properly detect selections
   public void getZRange(Vector2d zRange) {

      if (!isOrthogonal()) {
         zRange.x = myNear;
         zRange.y = myFar;
         return;
      }

      Point3d pmin = new Point3d();
      Point3d pmax = new Point3d();
      getBounds(pmin, pmax);

      // find max z depth
      Vector3d zdir = getZDirection();
      double worldDist = Math.abs(getEye().dot(zdir));
      double [] x = {pmin.x, pmax.x};
      double [] y = {pmin.y, pmax.y};
      double [] z = {pmin.z, pmax.z};
      double minz = Double.POSITIVE_INFINITY;
      double maxz = Double.NEGATIVE_INFINITY;
      for (int i=0; i<2; i++) {
         for (int j=0; j<2; j++) {
            for (int k=0; k<2; k++) {
               double d = x[i]*zdir.x + y[j]*zdir.y + z[k]*zdir.z;
               maxz = Math.max(maxz, d);
               minz = Math.min(minz, d);
            }
         }
      }

      // add 50% for good measure
      double d = maxz-minz;
      minz = minz-d/2;
      maxz = maxz+d/2;

      // XXX I have no idea why these are the correct formulae, but
      // they're the only ones that work (derived from a set of
      // test cases)
      zRange.y = maxz+worldDist;
      zRange.x = 2*(minz+worldDist)-zRange.y;

   }

   public void setViewVolume (double near, double far) {
      double aspect = width / (double)height;
      if (orthographicP) {
         double hh = fieldHeight / 2;
         gl.glOrtho (-aspect * hh, aspect * hh, -hh, hh, near, far);
      }
      else {
         if (explicitFrustumP) {
            gl.glFrustum (myLeft, myRight, myBottom, myTop, near, far);
         }
         else {
            myLeft = -aspect * myTop;
            myRight = -myLeft;
            gl.glFrustum (myLeft, myRight, myBottom, myTop, near, far);
         }

      }
   }

   private void resetViewVolume() {
      gl.glMatrixMode (GL2.GL_PROJECTION);
      gl.glLoadIdentity();
      setViewVolume (myNear, myFar);
      gl.glMatrixMode (GL2.GL_MODELVIEW);
      gl.glViewport (0, 0, width, height);
   }

   RigidTransform3d XEdgeToData = new RigidTransform3d();
   private FrameBufferObject FBO;
   private boolean grab;

   public void getWorldToEye (RigidTransform3d X) {
      X.invert (XEyeToWorld);
   }

   public RigidTransform3d getEyeToWorld() {
      return XEyeToWorld;
   }

   public void getEyeToWorld (RigidTransform3d X) {
      X.set (XEyeToWorld);
   }

   /**
    * Directly sets the eye coordinate frame.
    * 
    * @param X
    * new EyeToWorld transformation
    */
   public void setEyeToWorld (RigidTransform3d X) {
      XEyeToWorld.set (X);
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

      zaxis.sub (eye, center);
      zaxis.normalize();

      xaxis.cross (up, zaxis);
      double norm = xaxis.norm();

      if (norm > 1e-6) {
         xaxis.normalize();
         yaxis.cross (zaxis, xaxis);

         XEyeToWorld.R.setColumn (0, xaxis);
         XEyeToWorld.R.setColumn (1, yaxis);
         XEyeToWorld.R.setColumn (2, zaxis);
      }
      else {
         XEyeToWorld.R.setZDirection (zaxis);
      }
      XEyeToWorld.p.set (eye);
   }

   private void drawDragBox (GLAutoDrawable drawable) {
      gl.glMatrixMode (GL2.GL_PROJECTION);
      gl.glPushMatrix();
      gl.glLoadIdentity();
      gl.glMatrixMode (GL2.GL_MODELVIEW);
      gl.glPushMatrix();
      gl.glLoadIdentity();

      gl.glDisable (GL2.GL_LIGHTING);
      gl.glColor3f (0.5f, 0.5f, 0.5f);

      double x0 = 2 * myDragBox.x / (double)width - 1;
      double x1 = x0 + 2 * myDragBox.width / (double)width;
      double y0 = 1 - 2 * myDragBox.y / (double)height;
      double y1 = y0 - 2 * myDragBox.height / (double)height;

      gl.glBegin (GL2.GL_LINE_LOOP);
      gl.glVertex3d (x0, y0, 0);
      gl.glVertex3d (x1, y0, 0);
      gl.glVertex3d (x1, y1, 0);
      gl.glVertex3d (x0, y1, 0);
      gl.glEnd();

      gl.glEnable (GL2.GL_LIGHTING);

      gl.glPopMatrix();
      gl.glMatrixMode (GL2.GL_PROJECTION);
      gl.glPopMatrix();
      gl.glMatrixMode (GL2.GL_MODELVIEW);
   }

   public void display (GLAutoDrawable drawable) {
      int flags = myRenderFlags.get();
      display(drawable, flags);
   }

   private class RenderIterator implements Iterator<GLRenderable> {

      SortedRenderableList myList = null;
      int myListIdx = 0;
      int myIdx = -1;
      final int MAX_LIST_IDX = 7;

      RenderIterator() {
         myList = myInternalRenderList.myOpaque;
         myIdx = -1;
         myListIdx = 0;
         advance();
      }

      public boolean hasNext() {
         return myList != null;
      }

      public GLRenderable next() {
         if (myList == null) {
            throw new NoSuchElementException();
         }
         GLRenderable r = myList.get(myIdx);
         advance();
         return r;
      }

      public void remove() {
         throw new UnsupportedOperationException();
      }

      private SortedRenderableList getList (int idx) {
         switch (idx) {
            case 0: {
               return myInternalRenderList.myOpaque;
            }
            case 1: {
               return (myExternalRenderList != null ?
                  myExternalRenderList.myOpaque : null);
            }
            case 2: {
               return myInternalRenderList.myTransparent;
            }
            case 3: {
               return (myExternalRenderList != null ?
                  myExternalRenderList.myTransparent : null);
            }
            case 4: {
               return myInternalRenderList.myOpaque2d;
            }
            case 5: {
               return (myExternalRenderList != null ?
                  myExternalRenderList.myOpaque2d : null);
            }
            case 6: {
               return myInternalRenderList.myTransparent2d;
            }
            case 7: {
               return (myExternalRenderList != null ?
                  myExternalRenderList.myTransparent2d : null);
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

   Iterator<GLRenderable> renderIterator() {
      return new RenderIterator();
   }

   // XXX probably shouldn't synchronize the entire display operation
   private synchronized void display (GLAutoDrawable drawable, int flags) {

      // turn off buffer swapping when doing a selection render because
      // otherwise the previous buffer sometimes gets displayed
      drawable.setAutoSwapBufferMode (selectEnabled ? false : true);

      gl.glPushMatrix();
      if (selectEnabled) {
         mySelector.setupSelection (drawable);
      }
      doDisplay (drawable, flags);

      if (selectEnabled) {
         mySelector.processSelection (drawable);
         selectEnabled = false;
      }
      else {
         fireRerenderListeners();
      }
      gl.glPopMatrix();

      if (FBO != null && grab) {
         offscreenCapture (flags);

         grab = false;
      }
   }

   private void offscreenCapture (int flags) {
      if (!FBO.setup) {
         FBO.setupFBO ();
      }

      // Set rendering commands to go to offscreen frame buffer
      FBO.activate();

      // Initialize the OpenGL context FOR THE FBO
      gl.setSwapInterval (1);
      FBO.deactivate();
      FBO.activate();

      // Draw the scene into pbuffer
      gl.glPushMatrix();
      if (selectEnabled) {
         mySelector.setupSelection (drawable);
      }

      resetViewVolume = false;   //disable resetting view volume
      doDisplay (drawable, flags);

      if (selectEnabled) {
         mySelector.processSelection (drawable);
         selectEnabled = false;
      }
      else {
         fireRerenderListeners();
      }
      gl.glPopMatrix();
      // further drawing will go to screen
      FBO.deactivate();

      FBO.capture();
   }

   public void setBackgroundColor (float r, float g, float b) {
      setBackgroundColor(r, g, b, 1.0f);
   }

   public void setBackgroundColor(float r, float g, float b, float a) {
      bgColor[0] = r;
      bgColor[1] = g;
      bgColor[2] = b;
      bgColor[3] = a;
      bgColorRequest = true;
      repaint();
   }

   public void setBackgroundColor (Color color) {
      color.getComponents (bgColor);
      bgColorRequest = true;
      repaint();
   }

   public Color getBackgroundColor() {
      return new Color (bgColor[0], bgColor[1], bgColor[2]);
   }

   private void drawAxes (GL2 gl, double length) {
      double l = length;

      setLightingEnabled (false);

      // draw axis

      gl.glDepthFunc (GL2.GL_ALWAYS);

      if (!selectEnabled) {
         gl.glColor3f (1, 0, 0);
      }
      gl.glBegin (GL2.GL_LINES);
      gl.glVertex3d (l, 0.0, 0.0);
      gl.glVertex3d (0, 0.0, 0.0);
      gl.glEnd();

      if (!selectEnabled) {
         gl.glColor3f (0, 1, 0);
      }
      gl.glBegin (GL2.GL_LINES);
      gl.glVertex3d (0, l, 0.0);
      gl.glVertex3d (0, 0, 0.0);
      gl.glEnd();

      if (!selectEnabled) {
         gl.glColor3f (0, 0, 1);
      }
      gl.glBegin (GL2.GL_LINES);
      gl.glVertex3d (0, 0, l);
      gl.glVertex3d (0, 0, 0);
      gl.glEnd();

      gl.glDepthFunc (GL2.GL_LESS);

      setLightingEnabled (true);
   }

   private void enableTransparency (GL2 gl) {
      gl.glEnable (GL2.GL_BLEND);
      if (!alphaFaceCulling) {
         gl.glDepthMask (false);
         gl.glDisable (GL2.GL_CULL_FACE);
      }
      gl.glBlendFunc (sBlending.value(), dBlending.value());
   }

   public void setLightingEnabled (boolean enable) {
      if (!selectEnabled) {
         if (enable) {
            gl.glEnable (GL2.GL_LIGHTING);
         }
         else {
            gl.glDisable (GL2.GL_LIGHTING);
         }
      }
   }

   public boolean isLightingEnabled() {
      return gl.glIsEnabled (GL2.GL_LIGHTING);
   }

   private void disableTransparency (GL2 gl) {
      if (!alphaFaceCulling) {
         gl.glEnable (GL2.GL_CULL_FACE);
         gl.glDepthMask (true);
      }
      gl.glDisable (GL2.GL_BLEND);
   }

   public boolean isTransparencyEnabled() {
      return myTransparencyEnabledP;
   }

   public void setTransparencyEnabled (boolean enable) {

      // do not enable if in selection mode
      if (!(isSelecting() && enable)) {
         if (enable != myTransparencyEnabledP) {
            GL2 gl = drawable.getGL().getGL2();
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

   /*
    * set "up" vector for viewing matrix
    */
   public void setUpVector (Vector3d upVector) {
      myUp.set (upVector);
      setEyeToWorld (new Point3d (XEyeToWorld.p), myCenter, myUp);
   }

   public Vector3d getUpVector() {
      return myUp;
   }

   private void doDisplay (GLAutoDrawable drawable, int flags) {
      GL2 gl = drawable.getGL().getGL2();

      if (resetViewVolume && resizeEnabled) {
         resetViewVolume();
         resetViewVolume = false;
      }

      gl.glPushMatrix();

      if (isSelecting()) {
         gl.glClearColor (0f, 0f, 0f, 0f);  
      }
      else {
         gl.glClearColor (bgColor[0], bgColor[1], bgColor[2], bgColor[3]);
         if (bgColorRequest) {
            bgColorRequest = false;
         }
      }

      gl.glClear (GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

      RigidTransform3d X = new RigidTransform3d();
      X.invert (XEyeToWorld);
      GLSupport.transformToGLMatrix (GLMatrix, X);
      gl.glMultMatrixd (GLMatrix, 0);

      initLights();

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

      for (GLClipPlane cp : myClipPlanes) {
         if (cp.isClippingEnabled()) {
            cp.getPlaneValues (myClipPlaneValues);
            myClipPlaneValues[3] += cp.getOffset();
            gl.glClipPlane (cp.getFrontGlClipPlane(), myClipPlaneValues, 0);
            gl.glEnable (cp.getFrontGlClipPlane());
            if (cp.isSlicingEnabled()) {
               myClipPlaneValues[0] = -myClipPlaneValues[0];
               myClipPlaneValues[1] = -myClipPlaneValues[1];
               myClipPlaneValues[2] = -myClipPlaneValues[2];
               myClipPlaneValues[3] =
                  -myClipPlaneValues[3]+2*cp.getOffset ();   
               gl.glClipPlane (cp.getBackGlClipPlane(), myClipPlaneValues, 0);
               gl.glEnable (cp.getBackGlClipPlane());
            }
         }
      }

      gl.glPushMatrix();

      int qid = 0;
      qid = myInternalRenderList.renderOpaque (this, qid, flags);
      if (myExternalRenderList != null) {
         qid = myExternalRenderList.renderOpaque (this, qid, flags);
      }

      if (!isSelecting()) {
         enableTransparency (gl);
      }

      qid = myInternalRenderList.renderTransparent (this, qid, flags);
      if (myExternalRenderList != null) {
         qid = myExternalRenderList.renderTransparent (this, qid, flags);
      }

      disableTransparency (gl);

      gl.glPopMatrix();

      for (GLClipPlane cp : myClipPlanes) {
         if (cp.isClippingEnabled()) {
            gl.glDisable (cp.getFrontGlClipPlane());
            if (cp.isSlicingEnabled())
               gl.glDisable (cp.getBackGlClipPlane());
         }
      }

      // Draw 2D objects
      begin2DRendering(width, height);

      qid = myInternalRenderList.renderOpaque2d (this, qid, 0);
      if (myExternalRenderList != null) {
         qid = myExternalRenderList.renderOpaque2d (this, qid, 0);
      }

      enableTransparency (gl);
      qid = myInternalRenderList.renderTransparent2d (this, qid, 0);
      if (myExternalRenderList != null) {
         qid = myExternalRenderList.renderTransparent2d (this, qid, 0);
      }
      disableTransparency (gl);
      end2DRendering();

      gl.glPopMatrix();

      if (!isSelecting()) {
         if (myDragBox != null) {
            drawDragBox (drawable);
         }
      }

      gl.glFlush();
   }

   public void displayChanged (
      GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {

   }

   public static int getNameStackDepth (GL2 gl) {
      int[] depth = new int[1];
      gl.glGetIntegerv (GL2.GL_NAME_STACK_DEPTH, depth, 0);
      return depth[0];
   }

   public void mulTransform (RigidTransform3d X) {
      GLSupport.transformToGLMatrix (GLMatrix, X);
      getGL2().glMultMatrixd (GLMatrix, 0);
   }

   public void mulTransform (AffineTransform3d X) {
      GLSupport.transformToGLMatrix (GLMatrix, X);
      getGL2().glMultMatrixd (GLMatrix, 0);
   }

   public void getModelViewMatrix (Matrix4d X) {
      getGL2().glGetDoublev (GL2.GL_MODELVIEW_MATRIX, GLMatrix, 0);
      for (int i = 0; i < 4; i++) {
         for (int j = 0; j < 4; j++) {
            X.set (i, j, GLMatrix[j * 4 + i]);
         }
      }
   }

   public void getProjectionMatrix (Matrix4d X) {
      getGL2().glGetDoublev (GL2.GL_PROJECTION_MATRIX, GLMatrix, 0);
      for (int i = 0; i < 4; i++) {
         for (int j = 0; j < 4; j++) {
            X.set (i, j, GLMatrix[j * 4 + i]);
         }
      }
   }

   public void setModelViewMatrix (Matrix4d X) {
      getGL2().glMatrixMode (GL2.GL_MODELVIEW); // paranoid
      for (int i = 0; i < 4; i++) {
         for (int j = 0; j < 4; j++) {
            GLMatrix[j * 4 + i] = X.get (i, j);
         }
      }
      getGL2().glLoadMatrixd (GLMatrix, 0);
   }

   public void setProjectionMatrix (Matrix4d X) {
      gl.glMatrixMode (GL2.GL_PROJECTION);
      for (int i = 0; i < 4; i++) {
         for (int j = 0; j < 4; j++) {
            GLMatrix[j * 4 + i] = X.get (i, j);
         }
      }
      gl.glLoadMatrixd (GLMatrix, 0);
      gl.glMatrixMode (GL2.GL_MODELVIEW);
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

   private GLUquadric mySphereQuad;
   private GLUquadric myCylinderQuad;

   private boolean useDisplayLists = true;

   private float[] mySelectedColor = new float[] { 1f, 1f, 0 };
   private Material mySelectedMaterial =
      Material.createDiffuse (mySelectedColor, 1.0f, 32f);

   private float[] myCurrentColor;
   private Material myCurrentMaterial;
   //overrides diffuse color in myCurrentMaterial:
   private float[] myCurrentDiffuse; 

   private SelectionHighlighting myHighlighting = SelectionHighlighting.Color;

   public void setSelectionHighlighting (SelectionHighlighting mode) {
      myHighlighting = mode;
   }

   public SelectionHighlighting getSelectionHighlighting() {
      return myHighlighting;
   }

   public void setSelectionColor (Color color) {
      color.getRGBComponents (mySelectedColor);
      mySelectedMaterial = Material.createDiffuse (mySelectedColor, 1.0f, 32f);
   }

   public Color getSelectionColor() {
      return new Color (
         mySelectedColor[0], mySelectedColor[1], mySelectedColor[2]);
   }

   public Material getSelectionMaterial() {
      return mySelectedMaterial;
   }

   public void setColor (float[] rgb, boolean selected) {
      if (!selectEnabled) {
         if (selected && myHighlighting == SelectionHighlighting.Color) {
            myCurrentColor = mySelectedColor;
         }
         else if (rgb != null) {
            myCurrentColor = rgb;
         }
         if (myCurrentColor.length == 3) {
            gl.glColor3fv (myCurrentColor, 0);
         }
         else {
            gl.glColor4fv (myCurrentColor, 0);
         }
      }
   }

   public void setColor (float[] rgb) {
      if (!selectEnabled) {
         if (rgb != null) {
            myCurrentColor = rgb;
         }
         if (myCurrentColor.length == 3) {
            gl.glColor3fv (myCurrentColor, 0);
         }
         else {
            gl.glColor4fv (myCurrentColor, 0);
         }
      }
   }

   public void setColor (Color color) {
      if (!selectEnabled) {
         float[] rgba = color.getColorComponents (null);
         myCurrentColor = rgba;
         gl.glColor4fv (myCurrentColor, 0);
      }
   }

   public void updateColor (float[] rgb, boolean selected) {
      if (!selectEnabled) {
         float[] c;
         if (selected && myHighlighting == SelectionHighlighting.Color) {
            c = mySelectedColor;
         }
         else {
            c = rgb;
         }
         if (myCurrentColor != c) {
            myCurrentColor = c;
            if (myCurrentColor.length == 3) {
               gl.glColor3fv (myCurrentColor, 0);
            }
            else {
               gl.glColor4fv (myCurrentColor, 0);
            }
         }
      }
   }

   public void setColor (float r, float g, float b) {
      if (!selectEnabled) {
         gl.glColor3f (r, g, b);
      }
   }

   public void setColor (float r, float g, float b, float a) {
      if (!selectEnabled) {
         gl.glColor4f (r, g, b, a);
      }
   }

   public void setMaterial (Material material, boolean selected) {
      setMaterial (material, null, selected);
   }

   public void setMaterial (
      Material material, float[] diffuseColor, boolean selected) {
      if (selected && myHighlighting == SelectionHighlighting.Color) {
         myCurrentMaterial = mySelectedMaterial;
         myCurrentDiffuse = null;
         myCurrentMaterial.apply (gl);
      }
      else {
         myCurrentMaterial = material;
         myCurrentDiffuse = diffuseColor;
         myCurrentMaterial.apply (gl, diffuseColor);
      }
   }

   public void setMaterialAndShading (
      RenderProps props, Material mat, boolean selected) {
      setMaterialAndShading (props, mat, null, selected);
   }

   public void setMaterialAndShading (
      RenderProps props, Material mat, float[] diffuseColor, boolean selected) {

      if (selectEnabled) {
         return;
      }
      Shading shading = props.getShading();
      if (shading == Shading.NONE) {
         setLightingEnabled (false);
         myCurrentColor = null; // ensure color gets set in updateMaterial
         updateMaterial (props, mat, diffuseColor, selected);
      }
      else {
         if (shading != Shading.FLAT) {
            gl.glShadeModel (GL2.GL_SMOOTH);
         }
         myCurrentMaterial = null; // ensure material gets set in updateMaterial
         updateMaterial (props, mat, diffuseColor, selected);
      }
   }

   public void restoreShading (RenderProps props) {
      if (selectEnabled) {
         return;
      }
      Shading shading = props.getShading();
      if (shading == Shading.NONE) {
         setLightingEnabled (true);
      }
      else if (shading != Shading.FLAT) {
         gl.glShadeModel (GL2.GL_FLAT);
      }      
   }

   public void updateMaterial (
      RenderProps props, Material material, boolean selected) {
      updateMaterial (props, material, null, selected);
   }

   protected float[] myDiffuseColor = new float[4];

   public void updateMaterial (
      RenderProps props, Material mat, float[] diffuseColor, boolean selected) {

      if (selectEnabled) {
         return;
      }
      if (props.getShading() == Shading.NONE) {
         float[] c;
         if (selected && myHighlighting == SelectionHighlighting.Color) {
            c = mySelectedColor;
         }
         else if (diffuseColor != null) {
            myDiffuseColor[0] = diffuseColor[0];
            myDiffuseColor[1] = diffuseColor[1];
            myDiffuseColor[2] = diffuseColor[2];
            myDiffuseColor[3] = (float)props.getAlpha();
            c = myDiffuseColor;
         }
         else {
            c = mat.getDiffuse();
         }
         if (c != myCurrentColor) {
            myCurrentColor = c;
            if (myCurrentColor.length == 3) {
               gl.glColor3fv (myCurrentColor, 0);
            }
            else {
               gl.glColor4fv (myCurrentColor, 0);
            }
         }
      }
      else {
         Material m;
         float[] d;
         if (selected && myHighlighting == SelectionHighlighting.Color) {
            m = mySelectedMaterial;
            d = null;
         }
         else {
            m = mat;
            d = diffuseColor;
         }
         if (myCurrentMaterial != m || myCurrentDiffuse != d) {
            myCurrentMaterial = m;
            myCurrentDiffuse = d;
            myCurrentMaterial.apply (gl, d);
         }
      }
   }

   public void drawSphere (RenderProps props, float[] coords) {
      double r = props.getPointRadius();
      drawSphere(props, coords, r);
   }

   @Override
   public void validateInternalDisplayLists(RenderProps props) {
      validateSphereDisplayList(props);
      validateTaperedEllipsoidDisplayList(props);
   }

   public int validateSphereDisplayList(RenderProps props) {

      int displayList = props.getSphereDisplayList(gl);
      if (displayList < 1) {
         displayList = props.allocSphereDisplayList (gl);
         if (displayList > 0) {
            gl.glNewList (displayList, GL2.GL_COMPILE);
            if (mySphereQuad == null) {
               mySphereQuad = glu.gluNewQuadric();
            }
            int slices = props.getPointSlices();
            glu.gluSphere (mySphereQuad, 1.0, slices, slices / 2);
            gl.glEndList();
         }
      }
      return displayList;
   }

   public int validateTaperedEllipsoidDisplayList(RenderProps props) {

      int displayList = props.getTaperedEllipsoidDisplayList(gl);
      if (displayList < 1) {
         displayList = props.allocTaperedEllipsoidDisplayList (gl);

         if (displayList > 0) {
            gl.glNewList (displayList, GL2.GL_COMPILE);
            GLU glu = getGLU();
            if (myCylinderQuad == null) {
               myCylinderQuad = glu.gluNewQuadric();
            }
            double s0 = 0;
            double c0 = 1;
            int nslices = props.getLineSlices();
            for (int slice = 0; slice < nslices; slice++) {
               double ang = (slice + 1) * 2 * Math.PI / nslices;
               double c1 = Math.cos (ang);
               double s1 = Math.sin (ang);

               gl.glBegin (GL2.GL_QUAD_STRIP);
               for (int j = 0; j <= nslices / 2; j++) {
                  double h = j * 1.0 / (nslices / 2);
                  double r = 1 * Math.sin (h * Math.PI / 1.0);
                  double drdh = Math.PI / 1.0 * 1.0 * Math.cos (h * Math.PI / 1.0);
                  gl.glVertex3d (c0 * r, s0 * r, h);
                  gl.glNormal3d (c0, s0, -drdh);
                  gl.glVertex3d (c1 * r, s1 * r, h);
                  gl.glNormal3d (c1, s1, -drdh);
               }
               gl.glEnd();

               s0 = s1;
               c0 = c1;
            }
            gl.glEndList();
         }
      }
      return displayList;
   }

   public void drawSphere (RenderProps props, float[] coords, double r) {
      GL2 gl = drawable.getGL().getGL2();

      gl.glPushMatrix();
      checkAndPrintGLError();
      gl.glTranslatef (coords[0], coords[1], coords[2]);
      gl.glScaled (r, r, r);
      checkAndPrintGLError();

      boolean normalizeEnabled = gl.glIsEnabled (GL2.GL_NORMALIZE);
      gl.glEnable (GL2.GL_NORMALIZE);

      int displayList = props.getSphereDisplayList (gl);
      if (!useDisplayLists || displayList < 1) {
         if (useDisplayLists) {
            displayList = props.allocSphereDisplayList (gl);
         }
         if (displayList > 0) {
            gl.glNewList (displayList, GL2.GL_COMPILE);
         }
         if (mySphereQuad == null) {
            mySphereQuad = glu.gluNewQuadric();
         }
         int slices = props.getPointSlices();
         glu.gluSphere (mySphereQuad, 1.0, slices, slices / 2);

         if (displayList > 0) {
            gl.glEndList();
            checkAndPrintGLError();
            gl.glCallList(displayList);
            checkAndPrintGLError();
         }
      }
      else {
         gl.glCallList (displayList);
         checkAndPrintGLError();
      }

      if (!normalizeEnabled) {
         gl.glDisable (GL2.GL_NORMALIZE);
      }
      gl.glPopMatrix();
   }

   private void setTriangle (GL2 gl, float[] v0, float[] v1, float[] v2) {
      float ax = v1[0]-v0[0];
      float ay = v1[1]-v0[1];
      float az = v1[2]-v0[2];
      float bx = v2[0]-v0[0];
      float by = v2[1]-v0[1];
      float bz = v2[2]-v0[2];
      gl.glNormal3f (ay*bz-az*by, az*bx-ax*bz, ax*by-ay*bx);
      gl.glVertex3fv (v0, 0);
      gl.glVertex3fv (v1, 0);
      gl.glVertex3fv (v2, 0);
   }

   private void setQuad (GL2 gl, float[] v0, float[] v1, float[] v2, float[] v3) {
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

      gl.glNormal3f (nx, ny, nz);
      gl.glVertex3fv (v0, 0);
      gl.glVertex3fv (v1, 0);
      gl.glVertex3fv (v2, 0);
      gl.glVertex3fv (v3, 0);
   }

   public void drawHex (
      RenderProps props, double scale,
      float[] v0, float[] v1, float[] v2, float[] v3,
      float[] v4, float[] v5, float[] v6, float[] v7) {

      float cx = (v0[0]+v1[0]+v2[0]+v3[0]+v4[0]+v5[0]+v6[0]+v7[0])/8;
      float cy = (v0[1]+v1[1]+v2[1]+v3[1]+v4[1]+v5[1]+v6[1]+v7[1])/8;
      float cz = (v0[2]+v1[2]+v2[2]+v3[2]+v4[2]+v5[2]+v6[2]+v7[2])/8;

      float s = (float)scale;
      GL2 gl = drawable.getGL().getGL2();
      gl.glPushMatrix();
      gl.glTranslatef (cx*(1-s), cy*(1-s), cz*(1-s));
      gl.glScalef (s, s, s);

      boolean normalizeEnabled = gl.glIsEnabled (GL2.GL_NORMALIZE);
      if (!normalizeEnabled) {
         gl.glEnable (GL2.GL_NORMALIZE);
      }

      gl.glBegin (GL2.GL_QUADS);
      setQuad (gl, v0, v1, v2, v3);
      setQuad (gl, v1, v5, v6, v2);
      setQuad (gl, v5, v4, v7, v6);
      setQuad (gl, v4, v0, v3, v7);
      setQuad (gl, v3, v2, v6, v7);
      setQuad (gl, v0, v4, v5, v1);
      gl.glEnd ();

      if (!normalizeEnabled) {
         gl.glDisable (GL2.GL_NORMALIZE);
      }
      gl.glPopMatrix();
   }

   public void drawWedge (
      RenderProps props, double scale,
      float[] v0, float[] v1, float[] v2,
      float[] v3, float[] v4, float[] v5) {

      float cx = (v0[0]+v1[0]+v2[0]+v3[0]+v4[0]+v5[0])/6;
      float cy = (v0[1]+v1[1]+v2[1]+v3[1]+v4[1]+v5[1])/6;
      float cz = (v0[2]+v1[2]+v2[2]+v3[2]+v4[2]+v5[2])/6;

      float s = (float)scale;
      GL2 gl = drawable.getGL().getGL2();
      gl.glPushMatrix();
      gl.glTranslatef (cx*(1-s), cy*(1-s), cz*(1-s));
      gl.glScalef (s, s, s);

      boolean normalizeEnabled = gl.glIsEnabled (GL2.GL_NORMALIZE);
      if (!normalizeEnabled) {
         gl.glEnable (GL2.GL_NORMALIZE);
      }

      gl.glBegin (GL2.GL_QUADS);
      setQuad (gl, v0, v1, v4, v3);
      setQuad (gl, v1, v2, v5, v4);
      setQuad (gl, v2, v0, v3, v5);
      gl.glEnd ();

      gl.glBegin (GL2.GL_TRIANGLES);
      setTriangle (gl, v0, v2, v1);
      setTriangle (gl, v3, v4, v5);
      gl.glEnd ();

      if (!normalizeEnabled) {
         gl.glDisable (GL2.GL_NORMALIZE);
      }
      gl.glPopMatrix();

   }

   public void drawPyramid (
      RenderProps props, double scale,
      float[] v0, float[] v1, float[] v2,
      float[] v3, float[] v4) {

      float cx = (v0[0]+v1[0]+v2[0]+v3[0]+v4[0])/5;
      float cy = (v0[1]+v1[1]+v2[1]+v3[1]+v4[1])/5;
      float cz = (v0[2]+v1[2]+v2[2]+v3[2]+v4[2])/5;

      float s = (float)scale;
      GL2 gl = drawable.getGL().getGL2();
      gl.glPushMatrix();
      gl.glTranslatef (cx*(1-s), cy*(1-s), cz*(1-s));
      gl.glScalef (s, s, s);

      boolean normalizeEnabled = gl.glIsEnabled (GL2.GL_NORMALIZE);
      if (!normalizeEnabled) {
         gl.glEnable (GL2.GL_NORMALIZE);
      }

      gl.glBegin (GL2.GL_QUADS);
      setQuad (gl, v0, v3, v2, v1);
      gl.glEnd ();

      gl.glBegin (GL2.GL_TRIANGLES);
      setTriangle (gl, v0, v1, v4);
      setTriangle (gl, v1, v2, v4);
      setTriangle (gl, v2, v3, v4);
      setTriangle (gl, v3, v0, v4);
      gl.glEnd ();

      if (!normalizeEnabled) {
         gl.glDisable (GL2.GL_NORMALIZE);
      }
      gl.glPopMatrix();

   }

   public void drawTet (
      RenderProps props, double scale,
      float[] v0, float[] v1, float[] v2, float[] v3) {

      float cx = (v0[0]+v1[0]+v2[0]+v3[0])/4;
      float cy = (v0[1]+v1[1]+v2[1]+v3[1])/4;
      float cz = (v0[2]+v1[2]+v2[2]+v3[2])/4;

      float s = (float)scale;
      GL2 gl = drawable.getGL().getGL2();
      gl.glPushMatrix();
      gl.glTranslatef (cx*(1-s), cy*(1-s), cz*(1-s));
      gl.glScalef (s, s, s);

      boolean normalizeEnabled = gl.glIsEnabled (GL2.GL_NORMALIZE);
      if (!normalizeEnabled) {
         gl.glEnable (GL2.GL_NORMALIZE);
      }

      gl.glBegin (GL2.GL_TRIANGLES);
      setTriangle (gl, v0, v2, v1);
      setTriangle (gl, v2, v3, v1);
      setTriangle (gl, v3, v0, v1);
      setTriangle (gl, v0, v3, v2);
      gl.glEnd ();

      if (!normalizeEnabled) {
         gl.glDisable (GL2.GL_NORMALIZE);
      }
      gl.glPopMatrix();
   }

   public void drawTaperedEllipsoid (
      RenderProps props, float[] coords0, float[] coords1) {
      GL2 gl = getGL2();

      utmp.set (coords1[0] - coords0[0], coords1[1] - coords0[1], coords1[2]
         - coords0[2]);

      Xtmp.p.set (coords0[0], coords0[1], coords0[2]);
      Xtmp.R.setZDirection (utmp);
      gl.glPushMatrix();
      GLViewer.mulTransform (gl, Xtmp);
      double len = utmp.norm();

      gl.glScaled (props.getLineRadius(), props.getLineRadius(), len);

      boolean normalizeEnabled = gl.glIsEnabled (GL2.GL_NORMALIZE);
      gl.glEnable (GL2.GL_NORMALIZE);

      int displayList = props.getTaperedEllipsoidDisplayList (gl);

      if (!useDisplayLists || displayList < 1) {
         if (useDisplayLists) {
            displayList = props.allocTaperedEllipsoidDisplayList (gl);
         }
         if (displayList > 0) {
            gl.glNewList (displayList, GL2.GL_COMPILE);
         }

         GLU glu = getGLU();
         if (myCylinderQuad == null) {
            myCylinderQuad = glu.gluNewQuadric();
         }

         double s0 = 0;
         double c0 = 1;
         int nslices = props.getLineSlices();

         for (int slice = 0; slice < nslices; slice++) {
            double ang = (slice + 1) * 2 * Math.PI / nslices;
            double c1 = Math.cos (ang);
            double s1 = Math.sin (ang);

            gl.glBegin (GL2.GL_QUAD_STRIP);
            for (int j = 0; j <= nslices / 2; j++) {
               double h = j * 1.0 / (nslices / 2);
               double r = 1 * Math.sin (h * Math.PI / 1.0);
               double drdh = Math.PI / 1.0 * 1.0 * Math.cos (h * Math.PI / 1.0);
               gl.glVertex3d (c0 * r, s0 * r, h);
               gl.glNormal3d (c0, s0, -drdh);
               gl.glVertex3d (c1 * r, s1 * r, h);
               gl.glNormal3d (c1, s1, -drdh);
            }

            gl.glEnd();

            s0 = s1;
            c0 = c1;
         }
         if (displayList > 0) {
            gl.glEndList();
            gl.glCallList(displayList);
         }
      }
      else {
         gl.glCallList (displayList);
      }

      if (!normalizeEnabled) {
         gl.glDisable (GL2.GL_NORMALIZE);
      }
      gl.glPopMatrix();
   }

   public void drawSolidArrow (
      RenderProps props, float[] coords0, float[] coords1) {
      drawSolidArrow (props, coords0, coords1, /* capped= */true);
   }

   public void drawSolidArrow (
      RenderProps props, float[] coords0, float[] coords1, boolean capped) {
      //GL2 gl = getGL().getGL2();

      utmp.set (
         coords1[0]-coords0[0], coords1[1]-coords0[1], coords1[2]-coords0[2]);
      double len = utmp.norm();
      utmp.normalize();

      double arrowRad = 3*props.myLineRadius;
      double arrowLen = Math.min(2*arrowRad,len/2);

      ctmp[0] = coords1[0] - (float)(arrowLen*utmp.x);
      ctmp[1] = coords1[1] - (float)(arrowLen*utmp.y);
      ctmp[2] = coords1[2] - (float)(arrowLen*utmp.z);

      drawCylinder (props, coords0, ctmp, capped);
      drawCylinder (props, ctmp, coords1, /*capped=*/true, arrowRad, 0.0);
   }

   public void drawCylinder (
      RenderProps props, float[] coords0, float[] coords1) {
      drawCylinder (props, coords0, coords1, /* capped= */false);
   }

   public void drawCylinder (
      RenderProps props, float[] coords0, float[] coords1, boolean capped) {
      double r = props.getLineRadius();
      drawCylinder (props, coords0, coords1, capped, r, r);
   }

   public void drawCylinder (
      RenderProps props, float[] coords0, float[] coords1, boolean capped,
      double base, double top) {

      // drawing manually like this is 10x faster that gluCylinder, but has
      // no texture coordinates
      int nslices = props.getLineSlices();
      utmp.set (coords1[0] - coords0[0], coords1[1] - coords0[1], coords1[2]
         - coords0[2]);

      Xtmp.p.set (coords0[0], coords0[1], coords0[2]);
      Xtmp.R.setZDirection (utmp);
      gl.glPushMatrix();
      GLViewer.mulTransform (gl, Xtmp);

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

      // draw sides
      gl.glBegin(GL2.GL_QUAD_STRIP);

      double c1,s1;
      for (int i = 0; i <= nslices; i++) {
         c1 = cosBuff[i];
         s1 = sinBuff[i];
         gl.glNormal3d(c1, s1, 0);
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

   public void drawLine (
      RenderProps props, float[] coords0, float[] coords1, boolean selected) {
      drawLine (props, coords0, coords1, /* capped= */false, selected);
   }

   public void drawLine (
      RenderProps props, float[] coords0, float[] coords1, boolean capped,
      boolean selected) {
      drawLine (props, coords0, coords1, capped, null, selected);
   }

   public void drawLine (
      RenderProps props, float[] coords0, float[] coords1, boolean capped,
      float[] color, boolean selected) {
      GL2 gl = getGL().getGL2();
      switch (props.getLineStyle()) {
         case LINE: {
            setLightingEnabled (false);
            gl.glLineWidth (props.getLineWidth());
            if (color == null) {
               color = props.getLineColorArray ();
            }
            if (props.getAlpha () < 1) {
               color = new float[]{color[0], color[1], color[2], (float)props.getAlpha ()};
            }
            setColor (color, selected);
            gl.glBegin (GL2.GL_LINES);
            gl.glVertex3fv (coords0, 0);
            gl.glVertex3fv (coords1, 0);
            gl.glEnd();
            gl.glLineWidth (1);
            setLightingEnabled (true);
            break;
         }
         case CYLINDER: {
            setMaterialAndShading (props, props.getLineMaterial(), color, selected);
            drawCylinder (props, coords0, coords1, capped);
            restoreShading (props);
            break;
         }
         case SOLID_ARROW: {
            setMaterialAndShading (props, props.getLineMaterial(), color, selected);
            drawSolidArrow (props, coords0, coords1, capped);
            restoreShading (props);
            break;
         }
         case ELLIPSOID: {
            setMaterialAndShading (props, props.getLineMaterial(), color, selected);
            drawTaperedEllipsoid (props, coords0, coords1);
            restoreShading (props);
            break;
         }
         default: {
            throw new InternalErrorException (
               "Unimplemented line style " + props.getLineStyle());
         }
      }
   }

   public void drawArrow (
      RenderProps props, float[] coords0, float[] coords1, boolean capped,
      boolean selected) {
      GL2 gl = getGL().getGL2();

      utmp.set (coords1[0] - coords0[0], coords1[1] - coords0[1], coords1[2]
         - coords0[2]);
      double len = utmp.norm();

      utmp.normalize();
      vtmp.set (coords1[0], coords1[1], coords1[2]);
      double arrowheadSize = 3 * props.myLineRadius;
      vtmp.scaledAdd (-len + arrowheadSize, utmp);
      ctmp[0] = (float)vtmp.x;
      ctmp[1] = (float)vtmp.y;
      ctmp[2] = (float)vtmp.z;

      switch (props.getLineStyle()) {
         case LINE: {
            setMaterialAndShading (props, props.getLineMaterial(), selected);
            if (len <= arrowheadSize) {
               drawCylinder (props, coords1, coords0, capped, len, 0.0);
            }
            else {
               setLightingEnabled (false);
               gl.glLineWidth (props.getLineWidth());
               setColor (props.getLineColorArray(), selected);
               gl.glBegin (GL2.GL_LINES);
               gl.glVertex3fv (coords1, 0);
               gl.glVertex3fv (ctmp, 0);
               gl.glEnd();
               gl.glLineWidth (1);
               setLightingEnabled (true);
               drawCylinder (
                  props, ctmp, coords0, capped, arrowheadSize, 0.0);
            }
            restoreShading (props);
            break;
         }
         case CYLINDER: {
            setMaterialAndShading (props, props.getLineMaterial(), selected);
            if (len <= arrowheadSize) {
               drawCylinder (props, coords1, coords0, capped, len, 0.0);
            }
            else {
               drawCylinder (props, ctmp, coords1, capped);
               drawCylinder (
                  props, ctmp, coords0, capped, arrowheadSize, 0.0);
            }
            restoreShading (props);
            break;
         }
         case ELLIPSOID: {
            setMaterialAndShading (props, props.getLineMaterial(), selected);
            if (len <= arrowheadSize) {
               drawCylinder (props, coords1, coords0, capped, len, 0.0);
            }
            else {
               drawTaperedEllipsoid (props, coords0, coords1);
               drawCylinder (
                  props, ctmp, coords0, capped, arrowheadSize, 0.0);
            }
            restoreShading (props);
            break;
         }
         default: {
            throw new InternalErrorException (
               "Unimplemented line style " + props.getLineStyle());
         }
      }
   }

   public void drawPoint (RenderProps props, float[] coords, boolean selected) {
      switch (props.getPointStyle()) {
         case POINT: {
            setLightingEnabled (false);
            gl.glPointSize (props.getPointSize());
            setColor (props.getPointColorArray(), selected);
            gl.glBegin (GL2.GL_POINTS);
            gl.glVertex3fv (coords, 0);
            gl.glEnd();
            gl.glPointSize (1);
            setLightingEnabled (true);
            break;
         }
         case SPHERE: {
            setMaterialAndShading (props, props.getPointMaterial(), selected);
            drawSphere (props, coords);
            restoreShading (props);
            break;
         }
      }
   }

   public void drawPoints (
      RenderProps props, Iterator<? extends RenderablePoint> iterator) {

      switch (props.getPointStyle()) {
         case POINT: {
            setLightingEnabled (false);
            // draw regular points first
            gl.glPointSize (props.getPointSize());
            if (isSelecting()) {
               // don't worry about color in selection mode
               int i = 0;
               while (iterator.hasNext()) {
                  RenderablePoint pnt = iterator.next();
                  if (pnt.getRenderProps() == null) {
                     if (isSelectable (pnt)) {
                        beginSelectionQuery (i);
                        gl.glBegin (GL2.GL_POINTS);
                        gl.glVertex3fv (pnt.getRenderCoords(), 0);
                        gl.glEnd();
                        endSelectionQuery ();
                     }
                  }
                  i++;
               }
            }
            else {
               gl.glBegin (GL2.GL_POINTS);
               setColor (props.getPointColorArray(), false);
               while (iterator.hasNext()) {
                  RenderablePoint pnt = iterator.next();
                  if (pnt.getRenderProps() == null) {
                     updateColor (props.getPointColorArray(), pnt.isSelected());
                     gl.glVertex3fv (pnt.getRenderCoords(), 0);
                  }
               }
               gl.glEnd();
            }
            gl.glPointSize (1);
            setLightingEnabled (true);
            break;
         }
         case SPHERE: {
            setMaterialAndShading (props, props.getPointMaterial(), false);
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
            restoreShading (props);
         }
      }
   }

   public static void drawLineStrip (
      GLRenderer renderer, Iterable<float[]> vertexList, RenderProps props,
      boolean isSelected) {
      GL2 gl = renderer.getGL2().getGL2();

      LineStyle lineStyle = props.getLineStyle();
      switch (lineStyle) {
         case LINE: {
            renderer.setLightingEnabled (false);
            // draw regular points first
            gl.glLineWidth (props.getLineWidth());
            gl.glBegin (GL2.GL_LINE_STRIP);
            renderer.setColor (props.getLineColorArray(), isSelected);
            for (float[] v : vertexList) {
               gl.glVertex3fv (v, 0);
            }
            gl.glEnd();
            gl.glLineWidth (1);
            renderer.setLightingEnabled (true);
            break;
         }
         case ELLIPSOID:
         case SOLID_ARROW:
         case CYLINDER: {
            renderer.setMaterialAndShading (
               props, props.getLineMaterial(), isSelected);
            float[] v0 = null;
            for (float[] v1 : vertexList) {
               if (v0 != null) {
                  if (lineStyle == LineStyle.ELLIPSOID) {
                     renderer.drawTaperedEllipsoid (props, v0, v1);
                  }
                  else if (lineStyle == LineStyle.SOLID_ARROW) {
                     renderer.drawSolidArrow (props, v0, v1);
                  }
                  else {
                     renderer.drawCylinder (props, v0, v1);
                  }
               }
               else {
                  v0 = new float[3];
               }
               v0[0] = v1[0];
               v0[1] = v1[1];
               v0[2] = v1[2];
            }
            renderer.restoreShading (props);
         }
      }
   }

   public void drawLines (
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
                        gl.glBegin (GL2.GL_LINES);
                        gl.glVertex3fv (line.getRenderCoords0(), 0);
                        gl.glVertex3fv (line.getRenderCoords1(), 0);
                        gl.glEnd();
                        endSelectionQuery ();
                     }
                  }
                  i++;
               }
            }
            else {
               gl.glBegin (GL2.GL_LINES);
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
                     gl.glVertex3fv (line.getRenderCoords0(), 0);
                     gl.glVertex3fv (line.getRenderCoords1(), 0);
                  }
               }
               gl.glEnd();
            }
            gl.glLineWidth (1);
            setLightingEnabled (true);
            break;
         }
         case ELLIPSOID:
         case SOLID_ARROW:
         case CYLINDER: {
            // GLU glu = getGLU();
            setMaterialAndShading (
               props, props.getLineMaterial(), /*selected=*/false);
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
            restoreShading (props);
            break;
         }
      }
   }

   public void drawAxes (
      RenderProps props, RigidTransform3d X, double len, boolean selected) {
      GL2 gl = getGL().getGL2();
      Vector3d u = new Vector3d();
      setLightingEnabled (false);
      gl.glLineWidth (props.getLineWidth());
      if (selected) {
         setColor (null, selected);
      }
      gl.glBegin (GL2.GL_LINES);
      for (int i = 0; i < 3; i++) {
         if (!selected && !selectEnabled) {
            gl.glColor3f (i == 0 ? 1f : 0f, i == 1 ? 1f : 0f, i == 2 ? 1f : 0f);
         }
         gl.glVertex3d (X.p.x, X.p.y, X.p.z);
         X.R.getColumn (i, u);
         gl.glVertex3d (X.p.x + len * u.x, X.p.y + len * u.y, X.p.z + len * u.z);
      }
      gl.glEnd();
      gl.glLineWidth (1);
      setLightingEnabled (true);
   }

   public void drawAxes (
      RenderProps props, RigidTransform3d X, double [] len, boolean selected) {
      GL2 gl = getGL().getGL2();
      Vector3d u = new Vector3d();
      setLightingEnabled (false);
      gl.glLineWidth (props.getLineWidth());

      if (selected) {
         setColor (null, selected);
      }

      gl.glBegin (GL2.GL_LINES);
      for (int i = 0; i < 3; i++) {
         if (len[i] != 0) {
            if (!selected && !selectEnabled) {
               gl.glColor3f (
                  i == 0 ? 1f : 0f, i == 1 ? 1f : 0f, i == 2 ? 1f : 0f);
            }
            gl.glVertex3d (X.p.x, X.p.y, X.p.z);
            X.R.getColumn (i, u);
            gl.glVertex3d (
               X.p.x + len[i] * u.x, X.p.y + len[i] * u.y, X.p.z + len[i] * u.z);
         }
      }

      gl.glEnd();
      gl.glLineWidth (1);
      setLightingEnabled (true);
   }

   public void setFaceMode (RenderProps.Faces mode) {
      switch (mode) {
         case FRONT_AND_BACK: {
            gl.glDisable (GL2.GL_CULL_FACE);
            break;
         }
         case FRONT: {
            gl.glEnable (GL2.GL_CULL_FACE);
            gl.glCullFace (GL2.GL_BACK);
            break;
         }
         case BACK: {
            gl.glEnable (GL2.GL_CULL_FACE);
            gl.glCullFace (GL2.GL_FRONT);
            break;
         }
         case NONE: {
            gl.glEnable (GL2.GL_CULL_FACE);
            gl.glCullFace (GL2.GL_FRONT_AND_BACK);
            break;
         }
      }
   }

   public void setDefaultFaceMode() {
      gl.glEnable (GL2.GL_CULL_FACE);
      gl.glCullFace (GL2.GL_BACK);
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

   public Point3d getCenter() {
      return new Point3d (myCenter);
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
      myCenter.set (c);
      setEyeToWorld (new Point3d (XEyeToWorld.p), myCenter, myUp);
   }

   public Point3d getEye() {
      return new Point3d (XEyeToWorld.p);
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
      setEyeToWorld (eye, myCenter, myUp);
   }

   /**
    * Returns a transform from world coordinates to center coordinates, with the
    * axes aligned to match the current eyeToWorld transform. Seen through the
    * viewer, this will appear centered on the view frame with z pointing toward
    * the view, y pointing up, and x pointing to the right.
    */
   public RigidTransform3d getCenterToWorld() {
      RigidTransform3d X = new RigidTransform3d();
      X.R.set (XEyeToWorld.R);
      X.p.set (myCenter);
      return X;
   }

   private int glClipPlaneToIndex (int glClipPlane) {
      switch (glClipPlane) {
         case GL2.GL_CLIP_PLANE0:
            return 0;
         case GL2.GL_CLIP_PLANE1:
            return 1;
         case GL2.GL_CLIP_PLANE2:
            return 2;
         case GL2.GL_CLIP_PLANE3:
            return 3;
         case GL2.GL_CLIP_PLANE4:
            return 4;
         case GL2.GL_CLIP_PLANE5:
            return 5;
         default: {
            throw new InternalErrorException (
               "No index for clip plane " + glClipPlane);
         }
      }
   }

   private int indexToGlClipPlane (int index) {
      switch (index) {
         case 0:
            return GL2.GL_CLIP_PLANE0;
         case 1:
            return GL2.GL_CLIP_PLANE1;
         case 2:
            return GL2.GL_CLIP_PLANE2;
         case 3:
            return GL2.GL_CLIP_PLANE3;
         case 4:
            return GL2.GL_CLIP_PLANE4;
         case 5:
            return GL2.GL_CLIP_PLANE5;
         default: {
            throw new InternalErrorException (
               "No clip plane for index " + index);
         }
      }
   }

   public int allocGlClipPlane() {
      int idx = -1;
      for (int i = 0; i < myGlPlaneAllocated.length; i++) {
         if (myGlPlaneAllocated[i] == false) {
            idx = i;
            break;
         }
      }
      if (idx == -1) {
         return idx;
      }
      myGlPlaneAllocated[idx] = true;
      return indexToGlClipPlane (idx);
   }

   public void freeGlClipPlane (int glClipPlane) {
      myGlPlaneAllocated[glClipPlaneToIndex (glClipPlane)] = false;
   }

   public int numFreeGlClipPlanes() {
      int cnt = 0;
      for (int i = 0; i < myGlPlaneAllocated.length; i++) {
         if (myGlPlaneAllocated[i] == false) {
            cnt++;
         }
      } 
      return cnt;
   }

   public GLClipPlane addClipPlane () {
      return addClipPlane (null, 0);
   }

   public GLClipPlane addClipPlane (RigidTransform3d X, double size) {
      GLClipPlane clipPlane = new GLClipPlane();
      if (size <= 0) {
         size = centerDistancePerPixel()*getWidth()/2;
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
         clipPlane.releaseClipPlanes(); 
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

   public boolean clipPlanesAvailable() {
      return getNumClipPlanes() < getMaxClipPlanes();
   }

   public int getMaxClipPlanes() {
      return myGlPlaneAllocated.length;
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

   public GLLight addLight (
      float[] position, float[] ambient, float[] diffuse, float[] specular) {
      GLLight light = new GLLight (position, ambient, diffuse, specular);
      lightManager.addLight (light);
      return light;
   }

   public void removeLight (int i) {
      GLLight light = lightManager.getLight(i);
      if (light != null) {
         lightManager.removeLight(light);
      }
   }

   public void removeLight(GLLight light) {
      lightManager.removeLight(light);
   }

   public GLLight getLight (int i) {
      return lightManager.getLight (i);
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
    * @param file
    * @param format
    */
   public synchronized void setupScreenShot (
      int w, int h, int samples, File file, String format) {

      if (FBO == null) {
         FBO = new FrameBufferObject (w, h, file, format, gl);
      }
      else {

         if (FBO.width != w || FBO.height != h || FBO.samples != samples) {
            FBO.cleanup(); // for triggering resetting of buffers
         }
         FBO.samples = samples;
         FBO.width = w;
         FBO.height = h;
         FBO.file = file;
         FBO.format = format;
      }
      grab = true;
   }

   /**
    * Allows you explicitly enable or disable resizing of viewer
    * (may want to disable while recording video)
    */
   public void setResizeEnabled(boolean enabled) {
      resizeEnabled = enabled;
   }

   public boolean isResizeEnabled() {
      return resizeEnabled;
   }

   public synchronized void setupScreenShot (
      int w, int h, File file, String format) {

      if (FBO == null) {
         FBO = new FrameBufferObject (w, h, file, format, gl);
      }
      else {
         FBO.cleanup(); // for triggering resetting of buffers
         FBO.width = w;
         FBO.height = h;
         FBO.file = file;
         FBO.format = format;
      }
      grab = true;
   }

   public boolean grabPending() {
      return grab;
   }

   public synchronized void clearOffscreenBuffer () {
      FBO = null;
   }

   public BlendType getSBlending() {
      return sBlending;
   }

   public void setSBlending(BlendType glBlendValue) {
      sBlending = glBlendValue;
   }

   public BlendType getDBlending() {
      return dBlending;
   }

   public void setDBlending(BlendType glBlendValue) {
      dBlending = glBlendValue;
   }

   public void setRotationMode(RotationMode mode) {
      myRotationMode = mode;
   }

   public RotationMode getRotationMode() {
      return myRotationMode;
   }

   public void setAlphaFaceCulling(boolean enable) {
      alphaFaceCulling = enable;
   }

   public boolean getAlphaFaceCulling() {
      return alphaFaceCulling;
   }

   public Vector3d getZDirection() {
      XEyeToWorld.R.getColumn(2, zDir);
      return zDir;
   }

   public void setGlobalRenderFlags(int flags) {
      myRenderFlags.setDefault (flags);
   }

   public void begin2DRendering() {
      begin2DRendering(width,height);
   }

   @Override
   public void begin2DRendering(double w, double h) {

      int attribBits = 
         GL2.GL_ENABLE_BIT | GL2.GL_TEXTURE_BIT | GL2.GL_COLOR_BUFFER_BIT 
         |GL2.GL_DEPTH_BUFFER_BIT | GL2.GL_TRANSFORM_BIT;
      gl.glPushAttrib(attribBits);

      setLightingEnabled (false);
      gl.glDisable(GL2.GL_DEPTH_TEST);
      gl.glDisable(GL2.GL_CULL_FACE);

      gl.glMatrixMode(GL2.GL_TEXTURE);
      gl.glPushMatrix();
      gl.glLoadIdentity();
      gl.glMatrixMode(GL2.GL_PROJECTION);
      gl.glPushMatrix();
      gl.glLoadIdentity();
      glu.gluOrtho2D(0, w, 0, h);
      gl.glMatrixMode(GL2.GL_MODELVIEW);
      gl.glPushMatrix();
      gl.glLoadIdentity();
      rendering2d = true;

   }

   @Override
   public void end2DRendering() {

      gl.glMatrixMode(GL2.GL_TEXTURE);
      gl.glPopMatrix();
      gl.glMatrixMode(GL2.GL_PROJECTION);
      gl.glPopMatrix();
      gl.glMatrixMode(GL2.GL_MODELVIEW);
      gl.glPopMatrix();
      gl.glPopAttrib();
      rendering2d = false;
   }

   @Override
   public boolean is2DRendering() {
      return rendering2d;
   }

   public boolean isMultiSampleEnabled() {
      return myMultiSampleEnabled;
   }

   public int numSelectionQueriesNeeded() {
      int num = myInternalRenderList.numSelectionQueriesNeeded();
      if (myExternalRenderList != null) {
         num += myExternalRenderList.numSelectionQueriesNeeded();
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

   public void beginSubSelection (GLSelectable s, int idx) {
      if (selectEnabled) {
         mySelector.beginSelectionForObject (s, idx);
      }
   }

   public void endSubSelection () {
      if (selectEnabled) {
         mySelector.endSelectionForObject ();
      }
   }

   public void setSelectionFilter (GLSelectionFilter filter) {
      mySelectionFilter = filter;
   }

   public GLSelectionFilter getSelectionFilter () {
      return mySelectionFilter;
   }

   public boolean isSelectable (GLSelectable s) {
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

   @Override
   public void dispose(GLAutoDrawable arg0) {
      // XXX nothing?
   }

   @Override
   public GL2 getGL2() {
      return drawable.getGL().getGL2();
   }

   @Override
   public int checkGLError() {
      return gl.glGetError();
   }

   private static void printErr(String msg) {
      String fullClassName = Thread.currentThread().getStackTrace()[3].getClassName();            
      String className = fullClassName.substring(fullClassName.lastIndexOf(".") + 1);
      String methodName = Thread.currentThread().getStackTrace()[3].getMethodName();
      int lineNumber = Thread.currentThread().getStackTrace()[3].getLineNumber();
      System.err.println(className + "." + methodName + "():" + lineNumber + ": " + msg);
   }

   public void checkAndPrintGLError() {
      int err = gl.glGetError();
      if (err != GL.GL_NO_ERROR) {
         String msg = Error.gluErrorString(err);
         printErr(msg + " (" +err + ")");
      }
   }

}

