/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.driver;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.MouseInfo;
import java.util.ArrayList;
import java.util.LinkedList;

import javax.swing.JPopupMenu;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;

import artisynth.core.gui.selectionManager.SelectionManager;
import artisynth.core.workspace.RootModel;
import maspack.matrix.AxisAlignedRotation;
import maspack.matrix.AxisAngle;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.Vector2d;
import maspack.render.Dragger3d;
import maspack.render.IsRenderable;
import maspack.render.RenderList;
import maspack.render.Renderer;
import maspack.render.Renderer.AxisDrawStyle;
import maspack.render.Viewer.RotationMode;
import maspack.render.Renderer.HighlightStyle;
import maspack.render.GL.GLViewer.*;
import maspack.render.GL.GLViewer;
import maspack.render.Viewer;
import maspack.render.GL.GLMouseAdapter;
import maspack.render.GL.GLViewerFrame;
import maspack.render.GL.GLViewerPanel;
import maspack.widgets.ButtonMasks;
import maspack.widgets.GuiUtils;
import maspack.widgets.MouseBindings;
import maspack.widgets.PropertyDialog;
import maspack.widgets.ViewerPopupManager;
import maspack.properties.*;

/**
 * Manages all the viewers used for model rendering.
 */
public class ViewerManager extends SettingsBase {

   // basic attributes that are passed to all viewers by default

   public static Color DEFAULT_BACKGROUND_COLOR = Color.BLACK;
   private Color myBackgroundColor = DEFAULT_BACKGROUND_COLOR;
   
   public static Color DEFAULT_SELECTION_COLOR = Color.YELLOW;
   private Color mySelectionColor = DEFAULT_SELECTION_COLOR;

   public static AxisDrawStyle DEFAULT_AXIS_DRAW_STYLE = AxisDrawStyle.LINE;
   private AxisDrawStyle myAxisDrawStyle = DEFAULT_AXIS_DRAW_STYLE;

   public static double DEFAULT_AXIS_LENGTH_RADIUS_RATIO = 60;
   private double myAxisLengthRadiusRatio = DEFAULT_AXIS_LENGTH_RADIUS_RATIO;

   public static RotationMode DEFAULT_ROTATION_MODE = RotationMode.FIXED_VERTICAL;
   protected RotationMode myRotationMode = DEFAULT_ROTATION_MODE;
   
   public static final AxisAlignedRotation DEFAULT_AXIAL_VIEW =
      AxisAlignedRotation.X_Z;
   protected AxisAlignedRotation myDefaultAxialView = DEFAULT_AXIAL_VIEW;

   public static Vector2d DEFAULT_ELLIPTIC_CURSOR_SIZE = new Vector2d(10,10);
   protected Vector2d myEllipticCursorSize = 
      new Vector2d(DEFAULT_ELLIPTIC_CURSOR_SIZE);

   public static int DEFAULT_SURFACE_RESOLUTION = 32;
   protected int mySurfaceResolution = DEFAULT_SURFACE_RESOLUTION; 

   // OpenGL transparency attributes

   public static boolean DEFAULT_TRANSPARENCY_FACE_CULLING = false;
   private boolean myTransparencyFaceCulling = DEFAULT_TRANSPARENCY_FACE_CULLING;

   public static boolean DEFAULT_BLENDING = false;
   private boolean myTransparencyBlending = DEFAULT_BLENDING;

   public static BlendFactor DEFAULT_BLEND_SOURCE_FACTOR =
      BlendFactor.GL_SRC_ALPHA;
   private BlendFactor myBlendSourceFactor = DEFAULT_BLEND_SOURCE_FACTOR;

   public static BlendFactor DEFAULT_BLEND_DEST_FACTOR =
      BlendFactor.GL_ONE_MINUS_CONSTANT_ALPHA;
   private BlendFactor myBlendDestFactor = DEFAULT_BLEND_DEST_FACTOR;

   // Flags for special "refresh" rendering
   public static final int DEFAULT_REFRESH_FLAGS = Renderer.SORT_FACES;
   private int myRefreshRenderFlags = DEFAULT_REFRESH_FLAGS;

   private LinkedList<IsRenderable> myRenderables =
      new LinkedList<IsRenderable>();
   private LinkedList<Dragger3d> myDraggers = new LinkedList<Dragger3d>();

   private MouseBindings myMouseBindings = null;
   private MouseBindings myEffectiveMouseBindings = null;

   public static final double DEFAULT_MOUSE_WHEEL_ZOOM_SCALE = 10;
   private double myMouseWheelZoomScale = DEFAULT_MOUSE_WHEEL_ZOOM_SCALE;

   public static final boolean DEFAULT_DRAW_AXES = false;
   private boolean myDefaultDrawAxes = DEFAULT_DRAW_AXES;

   public static final boolean DEFAULT_ORTHOGRAPHIC_VIEW = false;
   private boolean myDefaultOrthographicView = DEFAULT_ORTHOGRAPHIC_VIEW;

   public static final boolean DEFAULT_DRAW_GRID = false;
   private boolean myDefaultDrawGrid = DEFAULT_DRAW_GRID;

   double myDefaultAxisLength = -1;

   RenderList myRenderList;

   public static PropertyList myProps = new PropertyList (ViewerManager.class);

   static {
      myProps.add (
         "backgroundColor",
         "background color", DEFAULT_BACKGROUND_COLOR);
      myProps.add (
         "selectionColor", 
         "color used to highlight selected components", DEFAULT_SELECTION_COLOR);
      myProps.add (
         "axisDrawStyle", "style used for renderering axes", 
         DEFAULT_AXIS_DRAW_STYLE);
      myProps.add (
         "axisLengthRadiusRatio", 
         "default length/radius ratio to be used when rendering solid axes",
         DEFAULT_AXIS_LENGTH_RADIUS_RATIO, "NS");
      myProps.add (
         "rotationMode", "method for interactive rotation",
         DEFAULT_ROTATION_MODE);

      //myProps.add ("axisLength", "length of rendered x-y-z axes", 0);

      myProps.add(
         "defaultAxialView", "axis-aligned view orientation", DEFAULT_AXIAL_VIEW);
      myProps.add(
         "ellipticCursorSize", "dimension of the elliptic cursor", 
         DEFAULT_ELLIPTIC_CURSOR_SIZE);
      myProps.add(
         "surfaceResolution", "resolution for built-in curved primitives", 
         DEFAULT_SURFACE_RESOLUTION);

      myProps.add(
         "transparencyFaceCulling", "allow transparency face culling", false);
      myProps.add(
         "transparencyBlending", "enable/disable transparency blending", false);
      myProps.add(
         "blendSourceFactor", "source transparency blending",
         DEFAULT_BLEND_SOURCE_FACTOR);
      myProps.add(
         "blendDestFactor", "destination transparency blending",
         DEFAULT_BLEND_DEST_FACTOR);
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

   private class PopupManager extends ViewerPopupManager {

      PopupManager (Viewer viewer) {
         super (viewer);
      }

      @Override
      public void actionPerformed (ActionEvent e) {
         String cmd = e.getActionCommand();
         if ("Set viewer properties".equals(cmd)) {
            PropertyDialog dialog = createPropertyDialog("OK Cancel");
            // dialog will be null if viewer does not implement HasProperties
            if (dialog != null) {
               dialog.setVisible (true);
            }
         }
         else if ("Set viewer grid properties".equals(cmd)) {
            PropertyDialog dialog = createGridPropertyDialog("OK Cancel");
            dialog.setVisible (true);
         }
         else if ("Refresh view".equals(cmd)) {
            myViewer.rerender(myRefreshRenderFlags);
         }
      }
   }

   private ArrayList<PopupManager> myPopupManagers =
      new ArrayList<PopupManager>();
   private ArrayList<GLViewer> myViewers =
      new ArrayList<GLViewer>();
   private ArrayList<MouseInputListener> myMouseListeners =
      new ArrayList<MouseInputListener>();
   private ArrayList<KeyListener> myKeyListeners =
      new ArrayList<KeyListener>();
   private boolean selectionEnabledP = true;
   private boolean selectOnPressP = false;
   private boolean ellipticSelectionP = false;

   public void setDefaultDrawAxes (boolean drawAxes) {
      myDefaultDrawAxes = drawAxes;
   }

   public boolean getDefaultDrawAxes() {
      return myDefaultDrawAxes;
   }

   public void setDefaultDrawGrid (boolean drawGrid) {
      myDefaultDrawGrid = drawGrid;
   }

   public boolean getDefaultDrawGrid() {
      return myDefaultDrawGrid;
   }

   public void setDefaultOrthographicView (boolean ortho) {
      myDefaultOrthographicView = ortho;
   }

   public boolean getDefaultOrthographicView() {
      return myDefaultOrthographicView;
   }

   public void setDefaultAxisLength (double l) {
      myDefaultAxisLength = l;
   }

   public double getDefaultAxisLength() {
      return myDefaultAxisLength;
   }

   private int numMouseButtons() {
      return MouseInfo.getNumberOfButtons();
   }

   /**
    * Effective mouse bindings are those which are actually passed to the
    * viewers. These will equal those specified using {@link
    * #setMouseBindings}, unless the specified binding are equal to {@link
    * MouseBindings#Default}, in which case the effective bindings are
    * determined automatically based on the number of available mouse buttons.
    */
   public MouseBindings getEffectiveMouseBindings() {
      return myEffectiveMouseBindings;
   }

   /**
    * Returns the mouse bindings specified using {@link #setMouseBindings}.
    */
   public MouseBindings getMouseBindings() {
      return myMouseBindings;
   }

   /**
    * Sets the mouse bindings to be used by the viewers. If the bindings are
    * equal to {@link MouseBindings#Default}, then the <i>effective</i>
    * bindings passed to the viewers are determined automatically based on the
    * number of available mouse buttons.
    */
   public void setMouseBindings (MouseBindings bindings) {
      if (!bindings.equals (myMouseBindings)) {
         myMouseBindings = new MouseBindings(bindings);         
         if (bindings.getName().equals ("Default")) {
            bindings = MouseBindings.createDefaultBindings();
         }
         if (!bindings.equals (myEffectiveMouseBindings)) {
            myEffectiveMouseBindings = new MouseBindings(bindings); 
            for (GLViewer v : myViewers) {
               setViewerMouseBindings (v, bindings);
            }
         }       
      }
   }

   /**
    * Set mouse bindings in a specific viewer
    */
   private void setViewerMouseBindings (GLViewer viewer, MouseBindings bindings) {
      int numButtons = numMouseButtons();
      GLMouseAdapter adapter = (GLMouseAdapter)viewer.getMouseHandler();
      if (adapter == null) {
         // XXX paranoid - don't think we need to do this
         adapter = new GLMouseAdapter(viewer);
         viewer.setMouseHandler(adapter);
      }
      bindings.apply (adapter, numButtons);
   }

   public double getMouseWheelZoomScale() {
      return myMouseWheelZoomScale;
   }

   public void setMouseWheelZoomScale (double scale) {
      if (scale != myMouseWheelZoomScale) {
         myMouseWheelZoomScale = scale;
         for (GLViewer v : myViewers) {
            v.getMouseHandler().setMouseWheelZoomScale (scale);
         }       
      }
   }

   public void addRenderable (IsRenderable r) {
      myRenderables.add (r);
      myRenderList = null;
   }

   public void removeRenderable (IsRenderable r) {
      myRenderables.remove (r);
      myRenderList = null;

   }

   public void clearRenderables() {
      myRenderables.clear();
      myRenderList = null;
   }

   public RenderList getRenderList() {
      if (myRenderList == null) {
         myRenderList = buildRenderList();
      }
      return myRenderList;
   }

   public void addDragger (Dragger3d d) {
      myDraggers.add (d);
      for (Viewer v : myViewers) {
         v.addDragger (d);
      }
   }

   public void removeDragger (Dragger3d d) {
      myDraggers.remove (d);
      for (Viewer v : myViewers) {
         v.removeDragger (d);
      }
   }

   public void clearDraggers() {
      myDraggers.clear();
      for (Viewer v : myViewers) {
         v.clearDraggers();
      }
   }

   public ViewerManager () {
      ViewerMouseInputListener listener = new ViewerMouseInputListener();
      myMouseBindings = MouseBindings.Default;
      myEffectiveMouseBindings = MouseBindings.createDefaultBindings();
      addMouseListener (listener);
   }

   private void doAddViewer (GLViewer viewer) {
      myViewers.add (viewer);
      myPopupManagers.add (new PopupManager (viewer));
      viewer.setSelectionEnabled (selectionEnabledP);
      viewer.setSelectOnPress (selectOnPressP);
      setViewerMouseBindings (viewer, getEffectiveMouseBindings());
      viewer.getMouseHandler().setMouseWheelZoomScale (myMouseWheelZoomScale);
      for (MouseInputListener l : myMouseListeners) {
         viewer.addMouseInputListener (l);
      }
      for (KeyListener l : myKeyListeners) {
         viewer.addKeyListener (l);
      }
   }

   public void addViewer (GLViewer viewer) {
      if (getPopupManager (viewer) == null) {
         doAddViewer (viewer);
      }
      else {
         throw new IllegalStateException ("Viewer already known to manager");
      }
   }

   public void setSelectionHighlightStyle (HighlightStyle mode) {
      for (Viewer v : myViewers) {
         v.setHighlightStyle (mode);
      }
   }

   public HighlightStyle getSelectionHighlightStyle() {
      if (myViewers.size() > 0) {
         return myViewers.get(0).getHighlightStyle();
      }
      else {
         return HighlightStyle.NONE;
      }
   }

   public boolean removeViewer (Viewer viewer) {
      int idx = myViewers.indexOf (viewer);
      if (idx != -1) {
         for (MouseInputListener l : myMouseListeners) {
            viewer.removeMouseInputListener (l);
         }
         for (KeyListener l : myKeyListeners) {
            viewer.removeKeyListener (l);
         }
         myViewers.remove (idx);
         myPopupManagers.remove (idx);
         return true;
      }
      else {
         return false;
      }
   }

   public int numViewers() {
      return myViewers.size();
   }

   public GLViewer getViewer (int idx) {
      return myViewers.get(idx);
   }

   private PopupManager getPopupManager (Viewer viewer) {
      for (PopupManager pm : myPopupManagers) {
         if (pm.getViewer() == viewer) {
            return pm;
         }
      }
      return null;
   }

   public void resetViewer (GLViewer viewer) {
      viewer.clearDraggers();
      for (Dragger3d d : myDraggers) {
         viewer.addDragger (d);
      }
      viewer.setExternalRenderList (getRenderList());
      if (myDefaultOrthographicView) {
         viewer.autoFitOrtho ();
      }
      else {
         viewer.autoFitPerspective ();
      }

      if (myDefaultDrawAxes) {
         if (myDefaultAxisLength > 0) {
            viewer.setAxisLength (myDefaultAxisLength);
         }
         else {
            viewer.setAxisLength (GLViewer.AUTO_FIT);
         }
      }
      viewer.setGridVisible (myDefaultDrawGrid);

      viewer.setBackgroundColor (myBackgroundColor);
      viewer.setHighlightColor (mySelectionColor);
      viewer.setAxisDrawStyle (myAxisDrawStyle);
      viewer.setAxisLengthRadiusRatio (myAxisLengthRadiusRatio);
      viewer.setRotationMode (getRotationMode());
      viewer.setEllipticCursorSize (getEllipticCursorSize());
      viewer.setSurfaceResolution (getSurfaceResolution());

      // OpenGL specific
      if (viewer instanceof GLViewer) {
         GLViewer glviewer = (GLViewer)viewer;
         glviewer.setTransparencyFaceCulling (myTransparencyFaceCulling);
         glviewer.setTransparencyBlending (myTransparencyBlending);
         glviewer.setBlendSourceFactor (myBlendSourceFactor);
         glviewer.setBlendDestFactor (myBlendDestFactor);
      }

   }

   // accessors for attributes

   public void setBackgroundColor (Color color) {
      myBackgroundColor = color;
      for (Viewer v : myViewers) {
         v.setBackgroundColor (color);
      }
   }

   public Color getBackgroundColor() {
      return myBackgroundColor;
   }

   public void setSelectionColor (Color color) {
      mySelectionColor = color;
      for (Viewer v : myViewers) {
         v.setHighlightColor (color);
      }
   }

   public Color getSelectionColor() {
      return mySelectionColor;
   }

   public void setAxisDrawStyle (AxisDrawStyle style) {
      myAxisDrawStyle = style;
      for (GLViewer v : myViewers) {
         v.setAxisDrawStyle (style);
      }
   }

   public AxisDrawStyle getAxisDrawStyle() {
      return myAxisDrawStyle;
   }

   public void setAxisLengthRadiusRatio (double ratio) {
      myAxisLengthRadiusRatio = ratio;
      for (Viewer v : myViewers) {
         v.setAxisLengthRadiusRatio (ratio);
      }
   }

   public double getAxisLengthRadiusRatio() {
      return myAxisLengthRadiusRatio;
   }

   public void setRotationMode (RotationMode mode) {
      myRotationMode = mode;
      for (Viewer v : myViewers) {
         v.setRotationMode (mode);
      }
   }

   public RotationMode getRotationMode() {
      return myRotationMode;
   }

   public void setDefaultAxialView (AxisAlignedRotation axialView) {
      if (axialView != myDefaultAxialView) {
         myDefaultAxialView = axialView;
         for (Viewer v : myViewers) {
            v.setAxialView (axialView);
         }
      }
   }

   public AxisAlignedRotation getDefaultAxialView() {
      return myDefaultAxialView;
   }

   public void setEllipticCursorSize (Vector2d size) {
      myEllipticCursorSize = size;
      for (Viewer v : myViewers) {
         v.setEllipticCursorSize (size);
      }
   }

   public Vector2d getEllipticCursorSize() {
      return myEllipticCursorSize;
   }

   public void setSurfaceResolution (int res) {
      mySurfaceResolution = res;
      for (Viewer v : myViewers) {
         v.setSurfaceResolution (res);
      }
   }

   public int getSurfaceResolution() {
      return mySurfaceResolution;
   }

   // accessors for GL transparency attributes

   public void setTransparencyFaceCulling (boolean enable) {
      myTransparencyFaceCulling = enable;
      for (Viewer v : myViewers) {
         if (v instanceof GLViewer) {
            ((GLViewer)v).setTransparencyFaceCulling (enable);
         }
      }
   }

   public boolean getTransparencyFaceCulling() {
      return myTransparencyFaceCulling;
   }

   public void setTransparencyBlending (boolean enable) {
      myTransparencyBlending = enable;
      for (Viewer v : myViewers) {
         if (v instanceof GLViewer) {
            ((GLViewer)v).setTransparencyBlending (enable);
         }
      }
   }

   public boolean getTransparencyBlending() {
      return myTransparencyBlending;
   }

   public void setBlendSourceFactor (BlendFactor factor) {
      myBlendSourceFactor = factor;
      for (Viewer v : myViewers) {
         if (v instanceof GLViewer) {
            ((GLViewer)v).setBlendSourceFactor (factor);
         }
      }
   }

   public BlendFactor getBlendSourceFactor() {
      return myBlendSourceFactor;
   }

   public void setBlendDestFactor (BlendFactor factor) {
      myBlendDestFactor = factor;
      for (Viewer v : myViewers) {
         if (v instanceof GLViewer) {
            ((GLViewer)v).setBlendDestFactor (factor);
         }
      }
   }

   public BlendFactor getBlendDestFactor() {
      return myBlendDestFactor;
   }

   /* -------------*/

   public void resetViewers (AxisAngle frontView) {
      AxisAlignedRotation view = 
         AxisAlignedRotation.getNearest (new RotationMatrix3d(frontView));
      for (GLViewer v : myViewers) {
         v.setAxialView (view);
         resetViewer (v);
      }
   }

   public void resetViewers (AxisAlignedRotation axialView) {
      for (GLViewer v : myViewers) {
         v.setAxialView (axialView);
         resetViewer (v);
      }
   }

   RenderList buildRenderList() {
      RenderList list = new RenderList();
      list.addIfVisibleAll (myRenderables);
      RootModel root = Main.getMain().getRootModel();
      if (root != null) {
	 // modified by Ian: add RootModel to the render list, 
	 // 	note that prerender is called by addIfVisible()
	 //root.prerender (list);
	 list.addIfVisible(root);
      }
      return list;
   }

   public void render() {
      // System.out.println("vm_render");
      myRenderList = buildRenderList();
      for (Viewer v : myViewers) {
         v.setExternalRenderList (myRenderList);
         v.rerender();
      }
   }
   
   /**
    * causes the repaint of the viewers
    * 
    */
   public void repaint() {
      for (Viewer v : myViewers) {
         v.repaint();
      }
   }

   // ===============================================

   /**
    * Set the cursor on all the viewers.
    * 
    * @param cursor
    * The type of cursor to display.
    */
   public void setCursor (Cursor cursor) {
      for (Viewer v : myViewers) {
         v.setScreenCursor (cursor);
      }
   }

   /**
    * Remove the specified mouse input listener from each of the viewers.
    */
   public void removeMouseListener (MouseInputListener listener) {
      for (Viewer v : myViewers) {
         v.removeMouseInputListener (listener);
      }

      myMouseListeners.remove (listener);
   }

   /**
    * Add a mouse input listener to each of the viewers.
    * 
    * @param listener listener to add
    */
   public void addMouseListener (MouseInputListener listener) {
      // add the listener to the list of listeners
      myMouseListeners.add (listener);

      // add the listener to each viewer
      for (Viewer v : myViewers) {
         v.addMouseInputListener (listener);
      }
   }

   public ArrayList<MouseInputListener> getMouseListeners() {
      return myMouseListeners;
   }

   /**
    * Remove the specified key listener from each of the viewers.
    */
   public void removeKeyListener (KeyListener listener) {
      for (Viewer v : myViewers) {
         v.removeKeyListener (listener);
      }
      myKeyListeners.remove (listener);
   }

   /**
    * Add a key listener to each of the viewers.
    * 
    * @param listener listener to add
    */
   public void addKeyListener (KeyListener listener) {
      // add the listener to the list of listeners
      myKeyListeners.add (listener);

      // add the listener to each viewer
      for (Viewer v : myViewers) {
         v.addKeyListener (listener);
      }
   }

   public ArrayList<KeyListener> getKeyListeners() {
      return myKeyListeners;
   }

   /**
    * Set whether or not selection is being allowed on all the existing viewers.
    * Iterates through the viewers setting thier specific selection values to
    * that of the ViewerManager.
    * 
    * @param selection
    * Whether or not selection is enabled.
    */
   public void setSelectionEnabled (boolean selection) {
      selectionEnabledP = selection;

      for (Viewer v : myViewers) {
         v.setSelectionEnabled (selectionEnabledP);
      }
   }

   /**
    * Returns true if viewer selection is enabled.
    * 
    * @return true if viewer selection is enabled
    */
   public boolean isSelectionEnabled() {
      return selectionEnabledP;
   }

   /**
    * Set whether or not selection is done when the mouse is pressed.
    * If enabled, this automatically diables "drag selection".
    * 
    * @param enable
    * Whether or not selection is enabled.
    */
   public void setSelectOnPress (boolean enable) {
      selectOnPressP = enable;

      for (Viewer v : myViewers) {
         v.setSelectOnPress (selectOnPressP);
      }
   }

   /**
    * Returns true if "select on press" is enabled for the viewers.
    * 
    * @return true if "select on press" is enabled
    */
   public boolean getSelectOnPress() {
      return selectOnPressP;
   }
   
   public void resetEllipticCursorSize () {
      for (Viewer v : myViewers) {
         v.resetEllipticCursorSize();
         v.repaint();
      }
   }  
   
   /**
    * Set whether or not elliptic selection is enabled in all the viewers.
    * 
    * @param enable
    * Whether or not elliptic selection is enabled.
    */
   public void setEllipticSelection (boolean enable) {
      ellipticSelectionP = enable;

      for (Viewer v : myViewers) {
         v.setEllipticSelection (enable);
         v.setEllipticCursorActive (enable);
      }
   }

   /**
    * Returns true if elliptic selection is enabled for the viewers.
    * 
    * @return true if elliptic selection is enabled.
    */
   public boolean getEllipticSelection() {
      return ellipticSelectionP;
   }
   
   

   /**
    * Find the Viewer (if any) associated with a particular component
    */
   public static GLViewer getViewerFromComponent (Component comp) {
      while (comp != null) {
         if (comp instanceof GLViewerPanel) {
            return ((GLViewerPanel)comp).getViewer();
         }
         else if (comp instanceof GLViewerFrame) {
            return ((GLViewerFrame)comp).getViewer();
         }
         else {
            comp = comp.getParent();
         }
      }
      return null;
   }

   /**
    * The mouse input listener for a Viewer.
    * 
    * This class needs to be instantiated once for the first Viewer that is
    * created, then afterwards any viewers that are created automatically get
    * this listener attached to them by the ViewerManager.
    */
   private class ViewerMouseInputListener extends MouseInputAdapter {

      public ViewerMouseInputListener() {
         super();
      }

      public void mousePressed (MouseEvent e) {
         SelectionManager selectionManager =
            Main.getMain().getSelectionManager();
         if (e.getModifiersEx() == ButtonMasks.getContextMenuMask()) {
            if (selectionManager.getNumSelected() == 0) {
               Viewer viewer = null;
               if (e.getSource() instanceof Component) {
                  viewer = getViewerFromComponent ((Component)e.getSource());
               }
               if (viewer != null) {
                  PopupManager pm = getPopupManager (viewer);
                  displayPopup (e, viewer, pm);
               }
            }
            else {
               selectionManager.displayPopup (e);
            }
         }
      }
      
      private void displayPopup (
         MouseEvent e, Viewer viewer, PopupManager pm) {
         JPopupMenu popup = new JPopupMenu();
         if (viewer instanceof HasProperties) {
            popup.add (
               GuiUtils.createMenuItem (
                  pm, "Set viewer properties", "Set properties for this viewer"));
         }
         popup.add (GuiUtils.createMenuItem (
            pm, "Set viewer grid properties",
            "Set properties for this viewer's grid"));
         popup.add (GuiUtils.createMenuItem (
            pm, "Refresh view",
            "Refresh the rendered view, sorting faces if necessary"));
         popup.show (e.getComponent(), e.getX(), e.getY());
      }

   } // end viewerMouseInputListener

   

}
